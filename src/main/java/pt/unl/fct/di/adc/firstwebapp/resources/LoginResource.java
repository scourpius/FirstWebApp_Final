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

	/**
	 * Logger object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	//private final KeyFactory userKeyFactory = datastore.newKeyFactory();

	private final Gson g = new Gson();

	/**
	 * Empty constructor, could be omitted
	 */
	public LoginResource() {

	}
	
	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLoginV3(LoginData data) {
		
		LOG.fine("Attempt to login user: " + data.username);

		//construct key from username, so we can then get the user from the datastore
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);
		
		if(user!= null) {
			String hashedPwd = user.getString("password");
			
			//if password matches, login
			if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
				AuthToken token = new AuthToken(data.username); //authentication token
				LOG.info("User logged in: "+data.username);
				
				return Response.ok(g.toJson(token)).entity("User sucessfully logged in").build();
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
	
	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLoginV4(LoginData data, 
			@Context HttpServletRequest request, 
			@Context HttpHeaders headers) {
		
		LOG.fine("Attempt to login user: " + data.username);

		//Generate keys
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username)) //which ancestor it belongs to
					.setKind("UserStats").newKey("counters");
		Key logKey = datastore.allocateId(datastore.newKeyFactory() //logKeys generated automatically
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserLog").newKey());
		
		Transaction tn = datastore.newTransaction();
		try {
			
			Entity user = tn.get(userKey);
			if(user != null) {
				
				//Retrieve user stats
				Entity stats = tn.get(ctrsKey);
				
				//if there arent any stats/counters yet, it initizalizes them
				if(stats == null) {					
					stats = Entity.newBuilder(ctrsKey)
							.set("user_stats_logins", 0L)
							.set("user_stats_failed", 0L)
							.set("user_first_login", Timestamp.now())
							.set("user_last_login", Timestamp.now())
							.build();				
				}
								
				// If password is correct
				// Then construct the logs
				String hashedPwd = user.getString("password");

				if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
					
					//Building logs
					Entity log = Entity.newBuilder(logKey)
							.set("user_login_ip", request.getRemoteAddr())
							.set("user_login_host", request.getRemoteHost())
							.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
							.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
							.set("user_login_time", Timestamp.now())
							.build();

					//Copy user statistics and update them - not great
					Entity uStats = Entity.newBuilder(ctrsKey)
							.set("user_stats_logins", 1L +stats.getLong("user_stats_logins"))
							.set("user_stats_failed", stats.getLong("user_stats_failed"))
							.set("user_first_login", stats.getTimestamp("user_first_login"))
							.set("user_last_login", Timestamp.now())
							.build();
							
					
					tn.put(log, uStats); //put instead of ad because we need it to overwrite the previous logs there
					tn.commit();
			
					
					//Return Token
					AuthToken token = new AuthToken(data.username);
					LOG.info("User logged in: "+data.username);
					return Response.ok(g.toJson(token)).build();
					
				}
				else {
					//Wrong password
					//Update stats again and overwrite them					
					Entity uStats = Entity.newBuilder(ctrsKey)
							.set("user_stats_logins", stats.getLong("user_stats_logins"))
							.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
							.set("user_first_login", stats.getTimestamp("user_first_login"))
							.set("user_last_login", stats.getTimestamp("user_last_login"))
							.set("user_last_attempt", Timestamp.now())
							.build();
					
					tn.put(uStats);
					tn.commit();
					
					LOG.warning("Wrong password for :" + data.username);
					return Response.status(Status.FORBIDDEN).entity("Wrong password").build();
				}			
			}else { //user == null
				LOG.warning("User " + data.username +" does not exist");
				return Response.status(Status.FORBIDDEN).entity("User " + data.username +" does not exist").build();
			}
			
		}catch(Exception e){
			tn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			
		}finally {
		
			if (tn.isActive()) { // some error might've ocurred that made it not be commited or rolled back
				tn.rollback();
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
				List<Date> loginDates = new ArrayList();
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