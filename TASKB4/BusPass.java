public class BusPass {
    private final String passId;
    private final String regNo;
    private final String name;
    private int routeNo;
    private double amount;

    public BusPass(String passId, String regNo, String name, int routeNo, double amount) {
        this.passId = passId;
        this.regNo = regNo;
        this.name = name;
        this.routeNo = routeNo;
        this.amount = amount;
    }

    public String getPassId() { return passId; }
    public String getRegNo() { return regNo; }
    public String getName() { return name; }
    public int getRouteNo() { return routeNo; }
    public void setRouteNo(int routeNo) { this.routeNo = routeNo; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return String.format("BusPass{passId='%s', regNo='%s', name='%s', route=%d, amount=%.2f}",
                passId, regNo, name, routeNo, amount);
    }
}