package com.cleargist.facebook;

import java.util.LinkedList;
import java.util.List;


public class User {
	private int userID;
	private List<Integer> friends;
	
	public User(int userID, List<Integer> friends) {
		this.userID = userID;
		this.friends = friends;
	}
	
	public User() {
		this.friends = new LinkedList<Integer>();
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
	
	public List<Integer> getFriends() {
		return this.friends;
	}
}
