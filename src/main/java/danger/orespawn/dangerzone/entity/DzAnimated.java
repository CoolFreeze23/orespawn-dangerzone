package danger.orespawn.dangerzone.entity;

/** What DangerZone's spinning, blinking and many-headed models ask of their creature. */
public interface DzAnimated {
    /** The spinner's angle in radians; DangerZone advances it each time it draws. */
    float getNewangle();

    /** Whether light number i is lit. */
    default boolean getBlink(int i) {
        return false;
    }

    default boolean dzAttacking() {
        return false;
    }

    default boolean dzFlying() {
        return false;
    }

    default boolean dzStaying() {
        return false;
    }

    default int getHead1Ext() {
        return 0;
    }

    default int getHead2Ext() {
        return 0;
    }

    default int getHead3Ext() {
        return 0;
    }
}
