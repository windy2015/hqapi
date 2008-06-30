package com.hyperic.hq.hqapi1;

/**
 * The Hyperic HQ API.
 *
 * This is the main entry point into the HQ Api.
 */
public class HQApi {

    UserApi _userApi;

    /**
     * @param host The hostname of the HQ Server to connect to.
     * @param port The port on the HQ server to connect to.
     * @param isSecure Set to true if connecting via SSL.
     * @param user The user to connect as.
     * @param password The password for the given user.
     */
    public HQApi(String host, int port, boolean isSecure, String user,
                 String password) {
        _userApi = new UserApi(host, port, isSecure, user, password);
    }

    /**
     * Operate on Users
     * 
     * @return The User API for operating on Users in the HQ system.
     */
    public UserApi getUserApi() {
        return _userApi;
    }
}
