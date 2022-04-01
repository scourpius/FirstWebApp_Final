package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	private	final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

	public RegisterResource( ) {}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegistration(RegisterData data) {
		LOG.fine("Register attempt by user: " + data.username);

		// Checks input data
		if(!data.validRegistration()) {
			LOG.warning("Invalid data for registration of user: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key statsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username))
				.setKind("UserStats").newKey("counters");

		Transaction txn = datastore.newTransaction();

		try {
			Entity user = txn.get(userKey);
			Entity stats;
			
			if(user != null) {
				txn.rollback();
				LOG.warning("Attemp to register user " + data.username + " that already exists.");
				return Response.status(Status.FORBIDDEN).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("email", data.email)
						.set("name", data.name)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("telefone", data.telefone)
						.set("telemovel", data.telemovel)
						.set("morada", data.morada)
						.set("nif", data.nif)
						.set("state", "INATIVO")
						.set("role", "USER")
						.set("userCreationTime", Timestamp.now())
						.build();
				
				stats = Entity.newBuilder(statsKey)
						.set("user_stats_logins", 0L)
						.set("user_stats_failed", 0L)
						.set("user_first_login", Timestamp.now())
						.set("user_last_login", Timestamp.now())
						.build();
			}

			txn.add(user, stats);
			txn.commit();
			
			LOG.fine("Registration of " + data.username + " successful.");
			return Response.ok("Registration of user " + data.username + " succesful").build();
		} finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
}