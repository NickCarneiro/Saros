package de.fu_berlin.inf.dpp.feedback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.project.AbstractSharedProjectListener;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.project.ISharedProjectListener;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.util.Utils;

/**
 * A Collector class that collects for each number of users the time they were
 * present in the same session, as well as the total number of users. The times
 * each number of users were present are accumulated.<br>
 * <br>
 * <code>
 * session.time.users.1=0.11            <br>
 * session.time.users.2=23.86           <br>
 * session.time.percent.users.1=1       <br>
 * session.time.percent.users.2=99      <br>
 * session.users.total=3
 * </code><br>
 * <br>
 * The example shows that there might have been three different users, but they
 * were never present altogether at the same time.
 * 
 * @author Lisa Dohrmann
 */
@Component(module = "feedback")
public class ParticipantCollector extends AbstractStatisticCollector {

    protected static final Logger log = Logger
        .getLogger(PermissionChangeCollector.class.getName());

    /** a map to contain the number of participants and the associated times */
    protected Map<Integer, Long> participantTimes = new HashMap<Integer, Long>();

    /** a set to contain all users that participated in the session */
    protected Set<User> users = new HashSet<User>();

    protected long timeOfLastEvent;

    /**
     * contains the current number of participants of the session at each point
     * in time
     */
    protected int currentNumberOfParticipants = 0;

    protected long sessionStart;
    protected long sessionTime;

    protected ISharedProjectListener projectListener = new AbstractSharedProjectListener() {

        @Override
        public void userJoined(User user) {
            ++currentNumberOfParticipants;
            // handle the event with the former number of users
            handleUserEvent(currentNumberOfParticipants - 1);
            // add the new user to the set
            users.add(user);

            log.info(Utils.prefix(user.getJID())
                + "joined. Session now contains " + currentNumberOfParticipants
                + " buddies.");
        }

        @Override
        public void userLeft(User user) {
            --currentNumberOfParticipants;
            // handle the event with the former number of users
            handleUserEvent(currentNumberOfParticipants + 1);

            log.info(Utils.prefix(user.getJID()) + "left. Session now contains "
                + currentNumberOfParticipants + " buddies.");
        }

    };

    public ParticipantCollector(StatisticManager statisticManager,
        SarosSessionManager sessionManager) {
        super(statisticManager, sessionManager);
    }

    /**
     * Handles events in which the number of participants changed (session
     * started, user joined, user left, session ended). The time difference to
     * the last event is stored, together with the number of participants that
     * were present in this time frame.<br>
     * If the same number of participants were together earlier in the session,
     * the time difference is added to the existing time frame.
     */
    protected void handleUserEvent(int numberOfParticipants) {
        Long timeDiff = System.currentTimeMillis() - timeOfLastEvent;
        Long time = participantTimes.get(numberOfParticipants);

        time = (time == null) ? timeDiff : time + timeDiff;
        participantTimes.put(numberOfParticipants, time);

        // store the time of this event
        timeOfLastEvent = System.currentTimeMillis();
    }

    @Override
    protected void processGatheredData() {
        // store the total amount of users that participated in the session
        data.setSessionUsersTotal(users.size());

        // store the number of users and associated times
        for (Entry<Integer, Long> e : participantTimes.entrySet()) {
            int percent = getPercentage(e.getValue(), sessionTime);
            data.setSessionTimeForUsers(e.getKey(),
                StatisticManager.getTimeInMinutes(e.getValue()));
            data.setSessionTimePercentForUsers(e.getKey(), percent);
        }
    }

    @Override
    protected void clearPreviousData() {
        participantTimes.clear();
        users.clear();
        currentNumberOfParticipants = 0;
        timeOfLastEvent = 0;
        super.clearPreviousData();
    }

    @Override
    protected void doOnSessionStart(ISarosSession sarosSession) {
        sarosSession.addListener(projectListener);

        sessionStart = System.currentTimeMillis();
        timeOfLastEvent = sessionStart;

        // add all users to the set and store the number of participants
        users.addAll(sarosSession.getParticipants());
        currentNumberOfParticipants = sarosSession.getParticipants().size();

        handleUserEvent(currentNumberOfParticipants);

        log.info("Session started: Current number of participants: "
            + currentNumberOfParticipants);
    }

    @Override
    protected void doOnSessionEnd(ISarosSession sarosSession) {
        sarosSession.removeListener(projectListener);

        sessionTime = Math.max(1, System.currentTimeMillis() - sessionStart);
        handleUserEvent(currentNumberOfParticipants);

        log.info("Session ended: Current number of participants: "
            + currentNumberOfParticipants);
    }

}
