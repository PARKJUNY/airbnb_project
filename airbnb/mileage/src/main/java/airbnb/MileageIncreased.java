package airbnb;

public class MileageIncreased extends AbstractEvent {

    private Long mileageId;
    private Long roomId;
    private Integer mileagePoint;
    private String status;

    public Long getMileageId() {
        return mileageId;
    }

    public void setMileageId(Long mileageId) {
        this.mileageId = mileageId;
    }
    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    public Integer getMileagePoint() {
        return mileagePoint;
    }

    public void setMileagePoint(Integer mileagePoint) {
        this.mileagePoint = mileagePoint;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
