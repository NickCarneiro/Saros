package de.fu_berlin.inf.dpp.net.business;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.UserListInfo;
import de.fu_berlin.inf.dpp.net.internal.UserListInfo.JoinExtensionProvider;
import de.fu_berlin.inf.dpp.net.internal.UserListInfo.UserListEntry;
import de.fu_berlin.inf.dpp.net.internal.XMPPReceiver;
import de.fu_berlin.inf.dpp.net.internal.XMPPTransmitter;
import de.fu_berlin.inf.dpp.observables.SessionIDObservable;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.ui.SarosUI;
import de.fu_berlin.inf.dpp.util.Utils;

/**
 * Business Logic for handling Invitation requests
 */
@Component(module = "net")
public class UserListHandler {

    private static final Logger log = Logger.getLogger(UserListHandler.class
        .getName());

    @Inject
    protected XMPPTransmitter transmitter;

    @Inject
    protected SarosSessionManager sessionManager;

    @Inject
    protected SarosUI sarosUI;

    protected final SessionIDObservable sessionIDObservable;

    public UserListHandler(XMPPReceiver receiver,
        SessionIDObservable sessionIDObservablePar,
        final JoinExtensionProvider userListExtProv) {
        this.sessionIDObservable = sessionIDObservablePar;
        // TODO SessionID-Filter
        receiver.addPacketListener(new PacketListener() {

            public void processPacket(Packet packet) {
                JID fromJID = new JID(packet.getFrom());

                log.debug("Inv" + Utils.prefix(fromJID) + ": Received userList");
                UserListInfo userListInfo = userListExtProv.getPayload(packet);

                if (userListInfo == null) {
                    log.warn("Inv" + Utils.prefix(fromJID)
                        + ": The received userList packet's"
                        + " payload is null.");
                    return;
                }

                ISarosSession sarosSession = sessionManager.getSarosSession();
                assert sarosSession != null;

                User fromUser = sarosSession.getUser(fromJID);

                if (fromUser == null || !fromUser.isHost()) {
                    log.error("Received userList from buddy who "
                        + "is not part of our session or is not host: "
                        + Utils.prefix(fromJID));
                    return;
                }

                // Adding new users
                User newUser;
                for (UserListEntry userEntry : userListInfo.userList) {

                    // Check if we already know this user
                    User user = sarosSession.getUser(userEntry.jid);

                    if (user == null) {
                        // This user is not part of our project
                        newUser = new User(sarosSession, userEntry.jid,
                            userEntry.colorID);
                        newUser.setPermission(userEntry.permission);
                        if (userEntry.invitationComplete)
                            newUser.invitationCompleted();

                        // Add him and send him a message, and tell him our
                        // colour
                        sarosSession.addUser(newUser);
                    } else {
                        // User already exists

                        // Check if the existing user has the colour that we
                        // expect
                        if (user.getColorID() != userEntry.colorID) {
                            log.warn("Received color id doesn't"
                                + " match known color id");
                        }

                        // Update his permission
                        user.setPermission(userEntry.permission);

                        // Update invitation status
                        if (userEntry.invitationComplete
                            && !user.isInvitationComplete()) {
                            sarosSession.userInvitationCompleted(user);
                        }
                    }
                }

                // Remove extra users
                // copy the JID of each userEntry in the userList into a list of
                // validJIDs
                ArrayList<JID> validJIDs = new ArrayList<JID>();
                for (UserListEntry userEntry : userListInfo.userList) {
                    validJIDs.add(userEntry.jid);
                }
                // for all participants in the sarosSession
                Collection<User> participants = sarosSession.getParticipants();
                for (User u : participants) {
                    // if the participant's JID is NOT in the list of validJIDs
                    if (validJIDs.contains(u.getJID()) == false) {
                        // log an informational message about removing the
                        // participant,
                        // including the parcitipant's human-readable name
                        log.info("Removing participant: "
                            + u.getHumanReadableName());

                        // remove the participant from the sarosSession
                        sarosSession.removeUser(u);
                    }
                }

                transmitter.sendUserListConfirmation(fromJID);
            }

        }, userListExtProv.getPacketFilter());
    }
}