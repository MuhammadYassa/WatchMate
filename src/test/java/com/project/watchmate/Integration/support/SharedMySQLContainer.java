package com.project.watchmate.Integration.support;

import org.testcontainers.mysql.MySQLContainer;

public class SharedMySQLContainer {

    public static final MySQLContainer INSTANCE;

    static {
        INSTANCE = new MySQLContainer("mysql:8.4");
        INSTANCE.start();
    }

    private SharedMySQLContainer() {}
}
