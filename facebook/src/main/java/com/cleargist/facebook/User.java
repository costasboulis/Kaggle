package com.cleargist.facebook;

import java.util.HashSet;




public class User {
	private int userID;
	private HashSet<Integer> friends;
	
	public User(int userID, HashSet<Integer> friends) {
		this.userID = userID;
		this.friends = friends;
	}
	
	public User() {
		this.friends = new HashSet<Integer>();
	}
	
	public void addFriend(int friendID) {
		friends.add(friendID);
	}
	
	public void setUserId(int userID) {
		this.userID = userID;
	}
	
	public int getUserID() {
		return this.userID;
	}
	
	public HashSet<Integer> getFriends() {
		return this.friends;
	}
}
