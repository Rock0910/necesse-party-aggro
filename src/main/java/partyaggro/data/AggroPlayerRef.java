package partyaggro.data;

/** A player known to the server, for the management list. */
public class AggroPlayerRef {
    public final long authentication;
    public final String name;
    public final long firstSeen;
    public final long lastOnline;

    public AggroPlayerRef(long authentication, String name, long firstSeen, long lastOnline) {
        this.authentication = authentication;
        this.name = name;
        this.firstSeen = firstSeen;
        this.lastOnline = lastOnline;
    }
}
