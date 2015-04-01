package org.apache.gearpump.example.transport.partitioner

import org.apache.gearpump.Message
import org.apache.gearpump.example.transport.PassRecord
import org.apache.gearpump.partitioner.Partitioner

class PassRecordPartitioner extends Partitioner{
  override def getPartition(msg: Message, partitionNum: Int, currentPartitionId: Int): Int = {
    msg.msg match {
      case passRecord: PassRecord =>
        (passRecord.vehicleId.hashCode & Integer.MAX_VALUE) % partitionNum
      case _ =>
        (msg.msg.hashCode() & Integer.MAX_VALUE) % partitionNum
    }
  }
}
