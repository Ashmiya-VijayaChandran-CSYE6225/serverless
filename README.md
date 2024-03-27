# serverless

## Serverless with Cloud Functions

This repository contains the infrastructure and code for implementing serverless architecture with Google Cloud Functions. It leverages Cloud Functions, Pub/Sub, Cloud SQL, and Terraform to create a scalable and event-driven system.

## Setup
- Create & Setup GitHub Repository
- Create a new private GitHub repository for infrastructure in the GitHub organization you created.
- The repository name must be serverless.
- Fork the GitHub repository into your namespace. All development work will be done on your fork.
- All code for Cloud Functions should now reside in this repository.
- Add an appropriate .gitignore to your repository.

## Cloud Function Implementation
- The Cloud function is invoked by Pub/Sub when a new user account is created.
- The Cloud function is responsible for:
- Emailing the user a link they can click to verify their email address. The verification link expires after 2 minutes. An expired link cannot be used to verify the user.
- Tracking the emails sent in a Cloud SQL instance.
  
## Pub/Sub
- Create a topic named verify_email and set up a subscription for the Cloud Function.
- Set the data retention period for the topic to be 7 days.
  
##  Web Application Updates
- Update the web application to publish a message to the Pub/Sub topic when a new user account is created.
- The payload (message) should be in JSON format with all relevant information needed by the Cloud Function to send an email to the user and track the link verification.
- All API calls from user accounts that have not been verified are blocked until the user completes the verification.
- Bind appropriate IAM role (cannot be admin) to publish a message to the topic.
  
## Infrastructure as Code with Terraform
- Use Terraform to create the Pub/Sub topic, subscription, IAM, service accounts, role bindings, Cloud Functions, and any other required resources.
- Grant the necessary roles to service accounts to mitigate potential permission issues during deployment.