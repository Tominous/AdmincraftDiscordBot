/*
 * * Copyright (C) 2018-2019 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.admincraft;

import net.dean.jraw.models.Submission;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Database {
    private final MariaDbPoolDataSource pool;

    public Database(Config config) throws SQLException {
        this.pool = new MariaDbPoolDataSource();
        this.pool.setDatabaseName("admincraft");
        this.pool.setMaxPoolSize(3);
        this.pool.setUser("admincraft");
        this.pool.setPassword(config.getDbPass());
        this.pool.initialize();

        try (Connection connection = pool.getConnection()) {
            Statement statement = connection.createStatement();
            PreparedStatement tableExists = connection.prepareStatement("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'admincraft' AND TABLE_NAME = ?;");
            tableExists.setString(1, "reddit_posts");
            ResultSet settingsExists = tableExists.executeQuery();
            if (!settingsExists.next()) {
                statement.execute("CREATE TABLE reddit_posts ( id VARCHAR(20) PRIMARY KEY NOT NULL );");
                statement.execute("CREATE INDEX POSTS_ID_INDEX ON reddit_posts (id);");
            }
            tableExists.close();
            statement.close();
        }
    }

    public void shutdown() {
        this.pool.close();
    }

    public List<Submission> processNew(List<Submission> posts) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            // TODO less hardcoded
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM reddit_posts WHERE id IN (?,?,?,?,?);");
            for (int x = 1; x <= 5; x++) {
                statement.setString(x, posts.get(x - 1).getId());
            }
            ResultSet result = statement.executeQuery();
            List<String> ids = new ArrayList<>(5);
            while (result.next()) {
                ids.add(result.getString("id"));
            }
            List<Submission> newPosts = posts.stream().filter(post -> !ids.contains(post.getId())).collect(Collectors.toList());
            statement.close();
            if (newPosts.isEmpty()) {
                return newPosts;
            }
            statement = connection.prepareStatement("INSERT INTO reddit_posts (id) VALUES (?);");
            for (Submission post : newPosts) {
                statement.setString(1, post.getId());
                statement.execute();
            }
            statement.close();
            return newPosts;
        }
    }
}
