package freshvend.iot;

/** Một lệnh xuất hàng backend giao cho thiết bị (GET /commands). */
public record Command(String commandKey, int dispenseId, int slotNo, int attemptNo) {}
