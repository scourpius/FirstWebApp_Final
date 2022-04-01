package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.adc.firstwebapp.util.LogoutData;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {

	private	final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());

	public LogoutResource( ) {}

	@DELETE
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(LogoutData data) {
		LOG.info("Logout attempt by user: " + data.username);
		
		if (!data.isValid()) {
			LOG.warning("Invalid data for logout of user: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.username);
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			
			if (user == null) {
				txn.rollback();

				return Response.status(Status.FORBIDDEN).entity("User doesn't exists.").build();
			}
			
			if (token == null) {
				txn.rollback();

				return Response.status(Status.FORBIDDEN).entity("User not logged in.").build();
			}
			
			txn.delete(tokenKey);
			txn.commit();
			
			LOG.fine("User " + data.username + " has been logged out sucessfully.");
			return Response.ok("User " + data.username + " has been logged out.").build();
		}finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	//Feeling cute might delete this later
	public void logoutUser(String username) {
		LogoutData d = new LogoutData(username);
		doLogout(d);
	}
}
