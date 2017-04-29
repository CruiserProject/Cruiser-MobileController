package demo.amastigote.com.djimobilecontrol.DataUtil;


import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;

public class WaypointMissionParams {
    private final float autoFlightSpeed = 0.0f;
    private final float maxFlightSpeed = 0.0f;

    private WaypointMissionGotoWaypointMode missionGotoWaypointMode;
    private WaypointMissionHeadingMode missionHeadingMode;
    private WaypointMissionFinishedAction missionFinishedAction;
    private WaypointMissionFlightPathMode missionFlightPathMode;

    public float getAutoFlightSpeed() {
        return autoFlightSpeed;
    }

    public float getMaxFlightSpeed() {
        return maxFlightSpeed;
    }

    public WaypointMissionGotoWaypointMode getMissionGotoWaypointMode() {
        return missionGotoWaypointMode;
    }

    public void setMissionGotoWaypointMode(WaypointMissionGotoWaypointMode missionGotoWaypointMode) {
        this.missionGotoWaypointMode = missionGotoWaypointMode;
    }

    public WaypointMissionHeadingMode getMissionHeadingMode() {
        return missionHeadingMode;
    }

    public void setMissionHeadingMode(WaypointMissionHeadingMode missionHeadingMode) {
        this.missionHeadingMode = missionHeadingMode;
    }

    public WaypointMissionFinishedAction getMissionFinishedAction() {
        return missionFinishedAction;
    }

    public void setMissionFinishedAction(WaypointMissionFinishedAction missionFinishedAction) {
        this.missionFinishedAction = missionFinishedAction;
    }

    public WaypointMissionFlightPathMode getMissionFlightPathMode() {
        return missionFlightPathMode;
    }

    public void setMissionFlightPathMode(WaypointMissionFlightPathMode missionFlightPathMode) {
        this.missionFlightPathMode = missionFlightPathMode;
    }
}
