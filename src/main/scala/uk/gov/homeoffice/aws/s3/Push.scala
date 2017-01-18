package uk.gov.homeoffice.aws.s3

sealed trait Push {
  type SizeMessage = String

  val message: String

  def formattedMessage(numberOfBytes: Long, message: SizeMessage => String): String = {
    val mb = numberOfBytes / 1048576.0

    if (mb < 1) {
      val kb = numberOfBytes / 1024.0

      if (kb < 1) {
        message(s"$numberOfBytes Bytes")
      } else {
        message(s"${kb.toLong} K")
      }
    } else {
      message(s"${mb.toLong} M")
    }
  }
}

object Push {
  case class Completed(key: String, numberOfBytes: Long, numberOfMilliseconds: Long) extends Push {
    val message = formattedMessage(numberOfBytes, size => f"Push completed for $key with size $size in ${numberOfMilliseconds / 1000.0}%.2f seconds")
  }

  case class CompletedPartially(key: String, numberOfBytes: Long, numberOfMilliseconds: Long) extends Push {
    val message = formattedMessage(numberOfBytes, size => f"Push partially completed for $key with size $size in ${numberOfMilliseconds / 1000.0}%.2f seconds")
  }

  case class Cancelled(key: String) extends Push {
    val message = s"Push cancelled for $key"
  }

  case class Failed(key: String) extends Push {
    val message = s"Push failed for $key"
  }
}