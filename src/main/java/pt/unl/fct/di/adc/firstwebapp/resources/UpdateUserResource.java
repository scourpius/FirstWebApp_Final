package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.adc.firstwebapp.util.PasswordUpdateData;

@Path("/update")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class UpdateUserResource {
	
	private	final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final Logger LOG = Logger.getLogger(UpdateUserResource.class.getName());
	
	private LoginResource r = new LoginResource();

	public UpdateUserResource( ) {}
	
	@PUT
	@Path("/password")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(PasswordUpdateData data) {
		LOG.info("Update of password attempt by user: " + data.username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.username);

		Transaction txn = datastore.newTransaction();
		try {
			//Check if token is still valid
			Entity token = txn.get(tokenKey);
		
			if (token == null || !r.isLogged(token)) {
				txn.rollback();
				LOG.warning("Attempt to change password of user " + data.username +" who isn't logged in");
				return Response.status(Status.FORBIDDEN).entity("User " + data.username +" isn't logged in").build();
			}
			
			if (!data.isValid()) {
				LOG.warning("Invalid data for update of password of user: " + data.username);
				return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
			}
			
			Entity user = txn.get(userKey);
			
			if(!user.getString("password").equals(DigestUtils.sha512Hex(data.oldPassword))) {
				txn.rollback();
				LOG.warning("Attempt to change user " + data.username+"'s password who used incorrect password.");
				return Response.status(Status.BAD_REQUEST).entity("Current password isn't correct.").build();
			}
			
			user = Entity.newBuilder(userKey)
					.set("email", user.getString("email"))
					.set("name", user.getString("name"))
					.set("password", DigestUtils.sha512Hex(data.newPassword))
					.set("telefone", user.getString("telefone"))
					.set("telemovel", user.getString("telemovel"))
					.set("morada", user.getString("morada"))
					.set("nif", user.getString("nif"))
					.set("state", user.getString("state"))
					.set("role", user.getString("role"))
					.set("userCreationTime",  user.getTimestamp("userCreationTime"))
					.build();
			
			txn.put(user);
			txn.delete(tokenKey);
			txn.commit();
			
			LOG.fine("User " + data.username + "'s password has been changed sucessfully.");
			return Response.ok("User " + data.username + " has changed his password.").build();
		}finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("[]").build();
			}
		}
	}
	
	@PUT
	@Path("/attributes")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateAttributes() {
		return null;
	}
	
}
