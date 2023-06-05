package loadManager.timeSlotManagement;

import mainPackage.PropertyFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sendable.TimeSlot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotBuilder {
    private static final Logger log = LogManager.getLogger(TimeSlotBuilder.class);
    private final long DURATION_IN_SECS;
    private final int MAX_NUM_TIME_SLOTS_SAVED;
    private List<TimeSlot> timeSlots;


    public TimeSlotBuilder() {
        PropertyFileReader propertyFileReader = new PropertyFileReader();
        this.DURATION_IN_SECS = Long.parseLong(propertyFileReader.getDuration());
        this.MAX_NUM_TIME_SLOTS_SAVED = Integer.parseInt(propertyFileReader.getMaxNumTimeSlotSaved());
    }


    /*needs to be called every duration
     * adds NUM_NEW_TIME_SLOTS */
    public TimeSlot addNewTimeSlot() {
        TimeSlot resultTimeSlot;
        if (timeSlots == null) {
            timeSlots = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            resultTimeSlot = addNewTimeSlot(now);
        } else {
            LocalDateTime start = timeSlots.get(timeSlots.size() - 1).getEndTime();
            resultTimeSlot = addNewTimeSlot(start);
        }
        return resultTimeSlot;
    }

    private TimeSlot addNewTimeSlot(LocalDateTime startTime) {
        LocalDateTime start = startTime;
        LocalDateTime end = start.plusSeconds(DURATION_IN_SECS);

        TimeSlot timeSlot = new TimeSlot(start, end);
        timeSlots.add(timeSlot);

        deleteOldTimeSlots();
        return timeSlot;
    }

    /*deletes old timeSlots if there are more than MAX_NUM_TIME_SLOTS_SAVED*/
    private void deleteOldTimeSlots() {
        while (timeSlots.size() > MAX_NUM_TIME_SLOTS_SAVED) {
            timeSlots.remove(0);
        }
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public LocalDateTime getLastSlotsEndtime() {
        if (timeSlots == null || timeSlots.size() == 0)
            return LocalDateTime.now();
        return timeSlots.get(timeSlots.size() - 1).getEndTime();
    }
}
