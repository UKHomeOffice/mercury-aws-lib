package uk.gov.homeoffice.aws.sqs

/**
  * Mixin to an Actor that is a SQSActor but SQS subscription switched off.
  * Why? When testing, you may need to test the functionality of your actor that is not related to actually needing to receive a SQS Message.
  */
trait SQSSubscriptionOff {
  this: SQSActor =>

  override def preStart(): Unit = ()
}