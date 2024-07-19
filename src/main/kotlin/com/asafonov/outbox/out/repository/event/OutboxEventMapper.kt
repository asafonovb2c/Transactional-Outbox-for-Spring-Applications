package com.asafonov.outbox.out.repository.event

import com.asafonov.outbox.domain.event.OutboxEvent
import com.asafonov.outbox.domain.event.OutboxEventStatus
import com.asafonov.outbox.domain.metric.OutboxEventTypeMetricsDto
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import java.time.Instant

@Mapper
interface OutboxEventMapper {

    /**
     * Deletes events from the queue by the unique event identifier.
     * @param events The collection of events to be deleted.
     */
    @Delete(
       """<script>
        DELETE 
        FROM outbox.event q 
        WHERE q.uuid = ANY ( VALUES
           <foreach item='item' collection='events' open='' separator=',' close='' > 
               (#{item.uuid}::uuid) 
           </foreach> 
        ) 
        </script>"""
    )
    fun delete(events: Collection<OutboxEvent>)

    /**
     * Inserts an event into the queue.
     * @param event The event to be inserted.
     */
    @Insert("""INSERT INTO 
            outbox.event(
               uuid, 
               event_type,
               create_timestamp, 
               run_time, 
               event, 
               attempts, 
               fail_reason, 
               lock_key, 
               status ) 
            VALUES (
               #{uuid}::uuid, 
               #{eventType}, 
               #{createTimestamp}, 
               #{runTime}, 
               #{event}, 
               #{attempts}, 
               #{failReason},
               #{lockKey},
               #{status})""")
    fun insert(event: OutboxEvent)

    @Insert("""<script>
            <foreach item='item' separator=';' collection='events'>
            INSERT INTO 
             outbox.event(
               uuid, 
               event_type,
               create_timestamp, 
               run_time, 
               event, 
               attempts, 
               fail_reason, 
               lock_key, 
               status ) 
             VALUES (
               #{item.uuid}::uuid,
               #{item.eventType}, 
               #{item.createTimestamp}, 
               #{item.runTime}, 
               #{item.event}, 
               #{item.attempts}, 
               #{item.failReason},
               #{item.lockKey}, 
               #{item.status})
            </foreach>
       </script>""")
    fun insertAll(events: Collection<OutboxEvent>)

    /**
     * Selects a specified number of the earliest events from the queue
     * that are scheduled for processing after a given time.
     *
     * @param eventType The type of the queue.
     * @param afterRunTime The time after which processing can begin for the events.
     * @param attemptsMax The maximum number of processing attempts for the event.
     * @param limit The maximum number of records to return.
     * @return A collection of queue events.
     */
    @Select("""
            SELECT * 
            FROM outbox.event 
            WHERE event_type=#{eventType} 
            AND status = #{status} 
            AND run_time <= #{afterRunTime} 
            AND attempts < #{attemptsMax} 
            ORDER BY run_time 
            LIMIT #{limit}
            """)
    fun selectEvents(eventType: String, status: OutboxEventStatus, afterRunTime: Instant,
                     attemptsMax: Long, limit: Int): Collection<OutboxEvent>


    /**
     * Selects a specified number of the earliest events from the queue
     * that are scheduled for processing after a given time.
     *
     * @param eventType The type of the queue.
     * @param afterRunTime The time after which processing can begin for the events.
     * @param attemptsMax The maximum number of processing attempts for the event.
     * @param limit The maximum number of records to return.
     * @return A collection of queue events.
     */
    @Select("""<script> 
            <if test='excludedUuids != null and !excludedUuids.isEmpty'>
            WITH excluded_uuids AS (
                VALUES ( 
                     <foreach item='item' index ='index' collection='excludedUuids' open='' separator=',' close=''>
                        (#{item}::uuid) 
                </foreach>
                )
            )
            </if>
            SELECT * 
            FROM outbox.event 
            <if test='excludedUuids != null and !excludedUuids.isEmpty'>
                LEFT JOIN excluded_uuids ON event.uuid = excluded_uuids.column1
            </if>
            WHERE  event_type = #{eventType} 
            AND status = #{status} 
            AND run_time &lt;= #{afterRunTime} 
            AND attempts &lt; #{attemptsMax} 
            <if test='excludedUuids != null and !excludedUuids.isEmpty'>
                AND excluded_uuids.column1 IS NULL
            </if>
            ORDER BY run_time 
            LIMIT #{limit}   
            </script>""")
    fun selectEventsWithoutUuids(eventType: String, status: OutboxEventStatus, afterRunTime: Instant, attemptsMax: Long,
                                 limit: Int, excludedUuids: Collection<String>): Collection<OutboxEvent>

    /**
     * Updates the runtime and other associated properties for the queue events.
     * @param events The events to be updated.
     */
    @Update("""<script>
               UPDATE outbox.event as q
               SET 
               run_time = vals.runTime, 
               attempts = vals.attempts, 
               fail_reason = vals.failReason, 
               status = vals.status 
               FROM (VALUES 
                     <foreach item='item' index ='index' collection='events' open='' separator=',' close=''>
                        ( #{item.uuid}::uuid, 
                          #{item.runTime}::timestamp with time zone,
                          #{item.createTimestamp}::timestamp with time zone, 
                          #{item.attempts}, 
                          #{item.failReason}, 
                          #{item.status} )
                     </foreach>
                      ) as vals(uuid, runTime, createTimestamp, attempts, failReason, status) 
               WHERE vals.uuid = q.uuid
               </script>
               """)
    fun updateEvents(events: List<OutboxEvent>)

    /**
     * Returns a list of DTOs for the event queues, where the 'type' parameter contains the event type names,
     * and the 'size' parameter indicates their current count.
     *
     * @return A list of event counts for each type.
     */
    @Select("""
        SELECT event_type as type, count(*) as size 
        FROM outbox.event 
        GROUP BY type
               """)
    fun selectCountEvents(): List<OutboxEventTypeMetricsDto>


}