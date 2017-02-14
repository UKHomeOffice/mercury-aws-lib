package uk.gov.homeoffice.aws.sqs

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.retry.PredefinedRetryPolicies
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class SQSSpec(implicit env: ExecutionEnv) extends Specification {
  "SQS" should {
    "configured" in new SQSServerEmbedded {
      override implicit val sqsClient: SQSClient = new SQSClient(sqsHost, new AnonymousAWSCredentials())(new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY))

      sqsClient.clientConfig.getRetryPolicy mustEqual PredefinedRetryPolicies.NO_RETRY_POLICY
    }

    "configured implicitly" in new SQSServerEmbedded {
      implicit val clientConfiguration = new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY)

      override implicit val sqsClient: SQSClient = new SQSClient(sqsHost, new AnonymousAWSCredentials())

      sqsClient.clientConfig.getRetryPolicy mustEqual PredefinedRetryPolicies.NO_RETRY_POLICY
    }
  }
}