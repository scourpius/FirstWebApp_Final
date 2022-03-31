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
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username))
					.setKind("UserStats").newKey("counters");
		Key logKey = datastore.allocateId(datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserLog").newKey());
		
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			
			if(user != null) {
				//Retrieve user statistics
				Entity stats = txn.get(ctrsKey);
				
				//if there aren't any statistics yet, it initializes them
				if(stats == null) {					
					stats = Entity.newBuilder(ctrsKey)
							.set("user_stats_logins", 0L)
							.set("user_stats_failed", 0L)
							.set("user_first_login", Timestamp.now())
							.set("user_last_login", Timestamp.now())
							.build();				
				}
								
				String hashedPwd = user.getString("password");

				if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
					Entity log = Entity.newBuilder(logKey)
							.set("user_login_ip", request.getRemoteAddr())
							.set("user_login_host", request.getRemoteHost())
							.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
							.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
							.set("user_login_time", Timestamp.now())
							.build();

					Entity uStats = Entity.newBuilder(ctrsKey)
							.set("user_stats_logins", 1L +stats.getLong("user_stats_logins"))
							.set("user_stats_failed", stats.getLong("user_stats_failed"))
							.set("user_first_login", stats.getTimestamp("user_first_login"))
							.set("user_last_login", Timestamp.now())
							.build();
							
					//Put instead of add to overwrite previous statistics
					txn.put(log, uStats);
					txn.commit();
					
					AuthToken token = new AuthToken(data.username);
					
					LOG.fine("User logged in: "+data.username);
					return Response.ok(g.toJson(token)).build();
					
				}
				else {
					//Wrong password		
					Entity uStats = Entity.newBuilder(ctrsKey)
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
			}else {
				//user == null
				LOG.warning("User " + data.username +" does not exist");
				return Response.status(Status.FORBIDDEN).entity("User " + data.username +" does not exist").build();
			}
			
		}catch(Exception e){
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			
		}finally {
		
			if (txn.isActive()) { 
				// some error might've occurred that made it not be committed or rolled back
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
		}
		
		
	}
	
	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response last24HLogins(LoginData data) {
		
		//get user from data
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);
		
		if(user!= null) {
			String hashedPwd = user.getString("password");
			
			//if password matches, login
			if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
				
				//Get yesterday's date
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -1);
				Timestamp yesterday = Timestamp.of(cal.getTime());
				
				//Define query				
				Query<Entity> query = Query.newEntityQueryBuilder()
						.setKind("UserLog")
						.setFilter(CompositeFilter.and(
								PropertyFilter.hasAncestor(datastore.newKeyFactory().setKind("User").newKey(data.username)),
								PropertyFilter.ge("user_login_time", yesterday)
								)
						)
						.build();
				
				//Run query
				QueryResults<Entity> logs = datastore.run(query);
				
				//List Results
				List<Date> loginDates = new ArrayList<>();
				logs.forEachRemaining(userlog -> {
					loginDates.add(userlog.getTimestamp("user_login_time").toDate());
				});
				
				
				return Response.ok(g.toJson(loginDates)).build();
			}
			else {
				LOG.warning("Wrong password for :" + data.username);
				return Response.status(Status.FORBIDDEN).entity("Wrong password").build();
			}
		}
		else {//username doesn't exist
			LOG.warning("User " + data.username +" does not exist");
			return Response.status(Status.FORBIDDEN).entity("User " + data.username +" does not exist").build();
		}
			
	}
}
