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
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();

		try {
			Entity user = txn.get(userKey);

			if(user != null) {
				txn.rollback();

				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("email", data.email)
						.set("name", data.name)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("telefone", data.telefone)
						.set("telemovel", data.telemovel)
						.set("morada", data.morada)
						.set("nif", data.nif)
						.set("userCreationTime", Timestamp.now())
						.build();
			}

			txn.add(user);
			txn.commit();
			
			LOG.fine("Registration of " + data.username + " successful.");
			return Response.ok("{}").build();
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
}