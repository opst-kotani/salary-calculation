package salarycalculation.domain;

import java.math.BigDecimal;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import salarycalculation.database.WorkDao;
import salarycalculation.entity.Capability;
import salarycalculation.entity.Employee;
import salarycalculation.entity.Organization;
import salarycalculation.entity.Role;
import salarycalculation.entity.Work;

/**
 * 社員情報ドメイン。
 *
 * @author naotake
 */
public class EmployeeDomain {

    /** 社員情報 */
    private Employee entity;

    /** 組織情報 */
    private Organization organization;
    /** 役割等級 */
    private Role role;
    /** 能力等級 */
    private Capability capability;

    private WorkDao workDao;

    /** 業務日付ドメイン */
    private BusinessDateDomain businessDateDomain;

    public EmployeeDomain(Employee entity) {
        this.entity = entity;
        this.workDao = new WorkDao();
        this.businessDateDomain = new BusinessDateDomain();
    }

    /**
     * 社員番号を取得する。
     *
     * @return 社員番号
     */
    public int getNo() {
        return entity.getNo();
    }

    /**
     * 入社年月日を基に勤続年数を取得する。<br />
     * 勤続年数が 1年未満の場合は 0 を返す。
     *
     * @return 勤続年数
     */
    // @UT
    public int getDurationYear() {
        int durationMonth = getDurationMonth();
        return (durationMonth / 12);
    }

    /**
     * 入社年月日を基に勤続月数を取得する。<br />
     * ここでいう勤続月数とは、現在日時点で勤続<b>何ヶ月目</b>かを表す。
     *
     * <pre>
     * 例えば
     * ・入社年月日: 2013/04/01
     * ・現在日時: 2014/04/01
     *
     * この場合、勤続月数は 13ヶ月目となる。
     *
     * ・現在日時: 2014/03/01 や 2014/03/31
     *
     * この場合、勤続月数は 12ヶ月目となる。
     * </pre>
     *
     * @return 勤続月数
     */
    // @UT
    public int getDurationMonth() {
        Calendar joinDateCal = Calendar.getInstance();
        joinDateCal.setTime(entity.getJoinDate());
        joinDateCal = DateUtils.truncate(joinDateCal, Calendar.HOUR);

        Calendar now = businessDateDomain.getNowAsCalendar();
        now = DateUtils.truncate(now, Calendar.HOUR);

        int count = 0;
        while (joinDateCal.before(now) || DateUtils.isSameDay(joinDateCal, now)) {
            joinDateCal.add(Calendar.MONTH, 1);
            count++;
        }

        if (count < 0) {
            count = 0;
        }

        return count;
    }

    /**
     * 指定年月の給料の手取り額を取得する。
     *
     * @param workYearMonth 稼動年月 (e.g. 201504)
     * @return 給料の手取り額
     */
    // @UT
    public int getTakeHomeAmount(int workYearMonth) {
        // 総支給額を求める
        int totalSalary = getTotalSalary(workYearMonth);

        // 控除額を求める
        int deduction = entity.getHealthInsuranceAmount() + entity.getEmployeePensionAmount()
                        + entity.getIncomeTaxAmount() + entity.getInhabitantTaxAmount();

        // 差引給与額を求める
        int takeHome = totalSalary - deduction;

        return takeHome;
    }

    /**
     * 指定年月の給料の総支給額を取得する。<br />
     * 総支給額の内訳は下記の通り。
     * <p />
     * 総支給額 = 基準内給与 (基本給 + 諸手当) + 基準外給与
     *
     * @param workYearMonth 稼動年月 (e.g. 201504)
     * @return 給料の総支給額
     */
    // @UT
    public int getTotalSalary(int workYearMonth) {
        int tmp = role.getAmount();
        tmp += capability.getAmount();
        tmp += getAllowance();
        tmp += getOvertimeAmount(workYearMonth);
        return tmp;
    }

    /**
     * 現在時点での諸手当を取得する。
     *
     * @return 諸手当
     */
    public int getAllowance() {
        // 諸手当を求める
        int allowance = entity.getCommuteAmount() + entity.getRentAmount();

        // 能力等級が 'PL' or 'PM' の場合、別途手当が出る
        if (StringUtils.equals(entity.getCapabilityRank(), "PL")) {
            allowance += 10000;
        } else if (StringUtils.equals(entity.getCapabilityRank(), "PM")) {
            allowance += 30000;
        }

        // 勤続年数が丸 3年目、5年目、10年目, 20年目の場合、別途手当が出る
        switch (getDurationYear()) {
        case 3:
            if ((getDurationMonth() % 12) == 0) {
                allowance += 3000;
            }
            break;
        case 5:
            if ((getDurationMonth() % 12) == 0) {
                allowance += 5000;
            }
            break;
        case 10:
            if ((getDurationMonth() % 12) == 0) {
                allowance += 10000;
            }
            break;
        case 20:
            if ((getDurationMonth() % 12) == 0) {
                allowance += 20000;
            }
            break;
        default:
            break;
        }

        return allowance;
    }

    /**
     * 指定年月の残業代を取得する。
     *
     * @param workYearMonth 稼動年月 (e.g. 201504)
     * @return 残業代
     */
    // @UT
    public int getOvertimeAmount(int workYearMonth) {

        int overtimeAmount = 0;

        // 稼動情報を取得
        Work work = workDao.getByYearMonth(entity.getNo(), workYearMonth);

        // 時間外手当を求める
        BigDecimal workOverTime1hAmount = BigDecimal.valueOf(entity.getWorkOverTime1hAmount());
        BigDecimal amount = workOverTime1hAmount.multiply(work.getWorkOverTime());
        int workOverTimeAllowance = amount.intValue();
        overtimeAmount += workOverTimeAllowance;

        // 深夜手当を求める
        workOverTime1hAmount = BigDecimal.valueOf(entity.getWorkOverTime1hAmount() * 1.1);
        amount = workOverTime1hAmount.multiply(work.getLateNightOverTime());
        int lateNightOverTimeAllowance = amount.intValue();
        overtimeAmount += lateNightOverTimeAllowance;

        // 休日手当を求める
        workOverTime1hAmount = BigDecimal.valueOf(entity.getWorkOverTime1hAmount() * 1.2);
        amount = workOverTime1hAmount.multiply(work.getHolidayWorkTime());
        int holidayWorkTimeAllowance = amount.intValue();
        overtimeAmount += holidayWorkTimeAllowance;

        // 休日深夜手当を求める
        workOverTime1hAmount = BigDecimal.valueOf(entity.getWorkOverTime1hAmount() * 1.3);
        amount = workOverTime1hAmount.multiply(work.getHolidayLateNightOverTime());
        int holidayLateNightOverTimeAllowance = amount.intValue();
        overtimeAmount += holidayLateNightOverTimeAllowance;

        // 能力等級が 'PL' or 'PM' の場合、残業代は出ない
        if (StringUtils.equals(entity.getCapabilityRank(), "PL")) {
            overtimeAmount = 0;
        } else if (StringUtils.equals(entity.getCapabilityRank(), "PM")) {
            overtimeAmount = 0;
        }

        return overtimeAmount;
    }

    /**
     * 現在の各等級を基に想定年収を取得する。<br />
     * 想定年収の内訳は下記の通り。
     * <p />
     * 想定年収 = 基準内給与 (基本給 + 諸手当) * 12
     * <p />
     * ただし、以下の諸手当は年収には含まれない。
     * <ul>
     * <li>通勤手当</li>
     * <li>住宅手当</li>
     * <li>勤続手当</li>
     * </ul>
     *
     * @return 想定年収
     */
    // @UT
    public int getAnnualTotalSalaryPlan() {
        // 基本給を求める
        int tmp = role.getAmount() + capability.getAmount();

        // 能力等級が 'PL' or 'PM' の場合、別途手当が出る
        if (StringUtils.equals(entity.getCapabilityRank(), "PL")) {
            tmp += 10000;
        } else if (StringUtils.equals(entity.getCapabilityRank(), "PM")) {
            tmp += 30000;
        }

        // 想定年収を求める
        tmp *= 12;

        return tmp;
    }

    public void setWorkDao(WorkDao workDao) {
        this.workDao = workDao;
    }

    public Employee getEntity() {
        return entity;
    }

    public void setEntity(Employee entity) {
        this.entity = entity;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(Capability capability) {
        this.capability = capability;
    }

    public void setBusinessDateDomain(BusinessDateDomain businessDateDomain) {
        this.businessDateDomain = businessDateDomain;
    }
}
