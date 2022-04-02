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
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.adc.firstwebapp.util.RemoveData;

@Path("/remove")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveResource {

	private	final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final Logger LOG = Logger.getLogger(RemoveResource.class.getName());
	
	private LoginResource r = new LoginResource();
	
	public RemoveResource( ) {}
	
	@DELETE
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRemove(RemoveData data) {
		LOG.info("Delete attemp for user: " + data.removed + " - by user " + data.remover);
		
		if (!data.isValid()) {
			LOG.warning("Invalid data for delete of user: " + data.removed);
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		Key removedKey = datastore.newKeyFactory().setKind("User").newKey(data.removed);
		Key removerKey = datastore.newKeyFactory().setKind("User").newKey(data.remover);
		Key statsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.removed))
				.setKind("UserStats").newKey("counters");
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.remover);
		Key tokenRemoveKey = datastore.newKeyFactory().setKind("Token").newKey(data.removed);

		Transaction txn = datastore.newTransaction();
		
		try {			
			Entity tokenEntity = txn.get(tokenKey);
			
			if (tokenEntity == null || !r.isLogged(tokenEntity)) {
				txn.rollback();
				LOG.warning("Attemp to remove user by logged out user: " + data.remover);
				return Response.status(Status.FORBIDDEN).entity("Attemp to remove user by logged out user: " + data.remover).build();
			}
			
			Entity removedUser = txn.get(removedKey);
			Entity removerUser = txn.get(removerKey);
			
			if (removedUser == null || removerUser == null) {
				txn.rollback();
				LOG.warning("Invalid data for removal of user: " + data.removed + " by user " +data.remover + ".");
				return Response.status(Status.FORBIDDEN).entity("User " +data.removed + " or user " + data.remover + " doesn't exists.").build();
			}
			
			if (!removedUser.toString().equals(removerUser.toString()) || !canRemove(removedUser, removerUser)) {
				txn.rollback();
				LOG.warning("Insuficient role by user " + data.remover + " to remove user " + data.removed + ".");
				return Response.status(Status.FORBIDDEN).entity("Insuficient role by user " + data.remover + " to remove user " + data.removed + ".").build();
			}
			
			//Delete user token if it exists
			Entity removeToken = txn.get(tokenRemoveKey);
			if (removeToken != null) {
				txn.delete(tokenRemoveKey);
			}
			
			txn.delete(removedKey, statsKey);
			txn.commit();
			
			LOG.fine("User " + data.removed + " has been deleted out sucessfully.");
			return Response.ok("User " + data.removed + " has been removed").build();
		}finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	
	private boolean canRemove(Entity removedUser, Entity removerUser) {
		String removedRole = removedUser.getString("role");
		String removerRole = removerUser.getString("role");
		
		if (removerRole.equals("SU"))
			return true;
		if ( removerRole.equals("GS") && ( removedRole.equals("USER") || removedRole.equals("GBO") ) )
			return true;
		if ( removerRole.equals("GBO") && removedRole.equals("USER"))
			return true;
		
		return true;
	}
}