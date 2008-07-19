package org.hyperic.hq.hqapi1.test;

import org.hyperic.hq.hqapi1.RoleApi;
import org.hyperic.hq.hqapi1.UserApi;
import org.hyperic.hq.hqapi1.types.Role;
import org.hyperic.hq.hqapi1.types.CreateRoleResponse;
import org.hyperic.hq.hqapi1.types.User;
import org.hyperic.hq.hqapi1.types.CreateUserResponse;
import org.hyperic.hq.hqapi1.types.SetUsersResponse;

import java.util.List;
import java.util.ArrayList;

public class RoleSetUsers_test extends RoleTestBase {

    public RoleSetUsers_test(String name) {
        super(name);
    }

    public void testSetUser() throws Exception {

        RoleApi roleApi = getRoleApi();
        Role r = generateTestRole();

        CreateRoleResponse createRoleResponse = roleApi.createRole(r);
        hqAssertSuccess(createRoleResponse);

        UserApi userApi = getUserApi();
        User user = generateTestUser();

        CreateUserResponse createUserResponse = userApi.createUser(user,
                                                                   PASSWORD);
        hqAssertSuccess(createUserResponse);

        Role role = createRoleResponse.getRole();
        List<User> users = new ArrayList<User>();
        users.add(user);
        SetUsersResponse setUserResponse = roleApi.setUsers(role, users);
        hqAssertSuccess(setUserResponse);
    }
}
