package com.neu.csye6225;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.cloudevents.CloudEvent;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Logger;

public class VerifyEmailPubSub implements CloudEventsFunction {
    private static final Logger logger = Logger.getLogger(VerifyEmailPubSub.class.getName());

    private static final String DATABASE_URL = System.getenv("DATABASE_URL");
    private static final String DATABASE_USERNAME = System.getenv("DATABASE_USERNAME");
    private static final String DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD");
    private static final String MAILGUN_API_KEY = System.getenv("MAILGUN_API_KEY");
    @Override
    public void accept(CloudEvent event) throws Exception {
        // Get cloud event data as JSON string
        String cloudEventData = new String(event.getData().toBytes());
        // Decode JSON event data to the Pub/Sub MessagePublishedData type
        Gson gson = new Gson();
        MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);
        // Get the message from the data
        Message message = data.getMessage();
        // Get the base64-encoded data from the message & decode it
        String encodedData = message.getData();
        String decodedData = new String(Base64.getDecoder().decode(encodedData));
        String[] token = decodedData.split(":");
        String verificationLink = "https://ashmiyavs.me/v9/user/verify-email?username="+token[0] + "&token="+token[1];
        String htmlContent = "<html><body>"
                + "<h1>Welcome, " + token[0] + "!</h1>"
                + "<p>Thank you for signing up. Please verify your email address by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify Email</a></p>"
                + "</body></html>";
        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/" + "ashmiyavs.me" + "/messages")
			    .basicAuth("api", MAILGUN_API_KEY)
                .queryString("from", "Sender Name <Sender@sender.com>")
                .queryString("to", token[0])
                .queryString("subject", "User Verification Email")
                .queryString("html", htmlContent)
                .asJson();
        updateMailSentTimestamp(token[0]);
        // Log the message
        logger.info("Pub/Sub message: " + decodedData);
    }

    private void updateMailSentTimestamp(String username) {
        try (Connection connection = establishConnection().getConnection()) {

            logger.info("In updateMailSentTimestamp method");

            String query = "UPDATE webapp.user set email_verify_expiry_time = DATE_ADD(NOW(), INTERVAL 2 MINUTE) where username=?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("User MailSentTimestamp updated successfully.");
            } else {
                logger.info("No records updated.");
            }
        } catch (SQLException e) {
            logger.info("SQL Exception is: " + e.getMessage());
        }
    }
    public DataSource establishConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(DATABASE_URL));
        config.setUsername(DATABASE_USERNAME);
        config.setPassword(DATABASE_PASSWORD);
        logger.info("Database URL:" + DATABASE_URL+" Database Username: "+DATABASE_USERNAME+" Database Password: "+DATABASE_PASSWORD);
        config.addDataSourceProperty("ipTypes", "PUBLIC,PRIVATE");
        return new HikariDataSource(config);
    }
}
