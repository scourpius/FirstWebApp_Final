package pt.unl.fct.di.adc.firstwebapp.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;

import java.util.logging.Logger;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public LoginResource() {}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLogin(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {
		LOG.info("Attempt to login user: " + data.username);
		
		if(!data.isValid()) {
			LOG.warning("Invalid data for login of user: " + data.username);
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key statsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username))
					.setKind("UserStats").newKey("counters");
		Key logKey = datastore.allocateId(datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserLog").newKey());
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.username);

		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			
			if(user == null) {
				txn.rollback();
				LOG.warning("Attemp to login to inexistent user: " + data.username);
				return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
			}else {
				//Retrieve user statistics
				Entity stats = txn.get(statsKey);
								
				String hashedPwd = user.getString("password");

				if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
					Entity tokenEntity = txn.get(tokenKey);
					
					if (tokenEntity != null) {
						txn.rollback();
						LOG.warning("Attemp to log in into logged in account: " + data.username);
						return Response.status(Status.FORBIDDEN).entity("User already logged in.").build();
					}
					
					Entity log = Entity.newBuilder(logKey)
							.set("user_login_ip", request.getRemoteAddr())
							.set("user_login_host", request.getRemoteHost())
							.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
							.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
							.set("user_login_time", Timestamp.now())
							.build();

					Entity uStats = Entity.newBuilder(statsKey)
							.set("user_stats_logins", 1L + stats.getLong("user_stats_logins"))
							.set("user_stats_failed", stats.getLong("user_stats_failed"))
							.set("user_first_login", stats.getTimestamp("user_first_login"))
							.set("user_last_login", Timestamp.now())
							.build();
					
					AuthToken token = new AuthToken(data.username);
					
					tokenEntity = Entity.newBuilder(tokenKey)
							.set("token_id", token.tokenID)
							.set("token_creation_time", token.creationTime)
							.set("token_expiration_time", token.expirationTime)
							.build();					
					
					
					//Put instead of add to overwrite previous statistics
					txn.put(log, uStats, tokenEntity);
					txn.commit();
										
					LOG.fine("User logged in: "+data.username);
					return Response.ok(g.toJson(token)).build();
					
				}
				else {
					//Wrong password		
					Entity uStats = Entity.newBuilder(statsKey)
							.set("user_stats_logins", stats.getLong("user_stats_logins"))
							.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
							.set("user_first_login", stats.getTimestamp("user_first_login"))
							.set("user_last_login", stats.getTimestamp("user_last_login"))
							.set("user_last_attempt", Timestamp.now())
							.build();
					
					txn.put(uStats);
					txn.commit();
					
					LOG.warning("Wrong password for :" + data.username);
					return Response.status(Status.FORBIDDEN).entity("Wrong password").build();
				}			
			}		
		}finally {
			if (txn.isActive()) { 
				// some error might've occurred that made it not be committed or rolled back
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}			
		}	
	}
	
	public boolean isLogged(Entity token) {
		long expirationTime = token.getLong("token_expiration_time");
		return expirationTime > System.currentTimeMillis();
	}
}