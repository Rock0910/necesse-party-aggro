import java.util.List;

import partyaggro.data.AggroConfig;
import partyaggro.data.AggroServerSection;
import partyaggro.data.AggroSource;

/** Plain-JDK tests for the pure logic in partyaggro.data. Not packaged into the mod jar. */
public class LogicTest {
    private static int passed = 0;

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FAILED: " + message);
        }
        passed++;
    }

    public static void main(String[] args) {
        mutualExclusion();
        effectiveHatred();
        clearAll();
        roundTrip();
        System.out.println("ALL TESTS PASSED (" + passed + " assertions)");
    }

    private static void mutualExclusion() {
        AggroServerSection s = new AggroServerSection();
        s.addHatred(11L, AggroSource.AUTO);
        check(s.isHatred(11L), "addHatred adds to hatred");
        check(!s.isWhitelist(11L), "addHatred not in whitelist");

        s.addWhitelist(11L);
        check(!s.isHatred(11L), "addWhitelist removes from hatred");
        check(s.sourceOf(11L) == null, "addWhitelist removes hatred source");
        check(s.isWhitelist(11L), "addWhitelist adds to whitelist");

        s.addHatred(11L, AggroSource.MANUAL);
        check(!s.isWhitelist(11L), "addHatred removes from whitelist");
        check(s.sourceOf(11L) == AggroSource.MANUAL, "source updated to MANUAL");
    }

    private static void effectiveHatred() {
        AggroServerSection s = new AggroServerSection();
        s.addHatred(1L, AggroSource.AUTO);
        s.addHatred(2L, AggroSource.MANUAL);
        s.addWhitelist(3L);
        check(s.effectiveHatred().size() == 2, "effectiveHatred size 2");
        check(s.effectiveHatred().contains(1L), "effectiveHatred contains 1");
        check(!s.effectiveHatred().contains(3L), "effectiveHatred excludes whitelist");
    }

    private static void clearAll() {
        AggroServerSection s = new AggroServerSection();
        s.addHatred(1L, AggroSource.AUTO);
        s.addHatred(2L, AggroSource.AUTO);
        s.addWhitelist(3L);
        s.clearAllHatred();
        check(s.hatred.isEmpty(), "clearAllHatred clears hatred");
        check(s.hatredSource.isEmpty(), "clearAllHatred clears sources");
        check(s.isWhitelist(3L), "clearAllHatred keeps whitelist");
    }

    private static void roundTrip() {
        AggroConfig config = new AggroConfig();
        config.setServerName(999L, "My Server");
        AggroServerSection s = config.get(999L);
        s.enabled = false;
        s.autoAddOnPlayerAttacks = false;
        s.addHatred(7L, AggroSource.AUTO);
        s.addWhitelist(8L);
        s.firstSeen.put(7L, 123456789L);

        List<String> lines = config.serialize();
        AggroConfig restored = new AggroConfig();
        restored.deserialize(lines);

        check("My Server".equals(restored.serverName(999L)), "server name round trip");
        AggroServerSection r = restored.getIfPresent(999L);
        check(r != null, "section round trip exists");
        check(!r.enabled, "enabled round trip");
        check(!r.autoAddOnPlayerAttacks, "autoAddOnPlayerAttacks round trip");
        check(!r.autoAddOnPlayerAttacked, "untouched flag keeps new default (off)");
        check(r.isHatred(7L), "hatred round trip");
        check(r.sourceOf(7L) == AggroSource.AUTO, "source round trip");
        check(r.isWhitelist(8L), "whitelist round trip");
        check(Long.valueOf(123456789L).equals(r.firstSeen.get(7L)), "firstSeen round trip");
    }
}
