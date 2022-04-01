package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {
	
	public static final long EXPIRATION_TIME = 1000 * 60 * 15; // 15 minutes
	
	public String username;
	public String tokenID;
	public long creationTime;
	public long expirationTime;

	public AuthToken(String username) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.creationTime = System.currentTimeMillis();
		this.expirationTime = this.creationTime + AuthToken.EXPIRATION_TIME;
	}
}