package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.orm.PostgreSQLStringArrayUserType

/**
 * 借用违规情况
 */
class BookingMisconduct {
    /**
     * 教室借用单
     */
    BookingItem bookingItem

    /**
     * 违规类别
     */
    String type

    /**
     * 违规内容
     */
    String content

    /**
     * 周次
     */
    Integer week

    /**
     * 实际人数
     */
    Integer userCount

    /**
     * 违规照片
     */
    String[] pictures

    /**
     * 报告人
     */
    Teacher reporter

    /**
     * 创建时间
     */
    Date dateCreated

    /**
     * 修改时间
     */
    Date dateModified

    /**
     * 初步意见
     */
    String handleOutcome

    /**
     * 处理人
     */
    Teacher handler

    /**
     * 处理时间
     */
    Date dateHandled

    /**
     * 学院意见
     */
    String checkOutcome

    /**
     * 审核人
     */
    Teacher checker

    /**
     * 审核时间
     */
    Date dateChecked

    /**
     * 处理结果
     */
    String approvalOutcome

    /**
     * 审批人
     */
    Teacher approver

    /**
     * 审批时间
     */
    Date dateApproved

    /**
     * 状态-0:待处理;1:待学院核实;2:学院已处理;3:已核实;4:不违规
     */
    Integer status

    static belongsTo = [
            bookingItem: BookingItem
    ]

    static mapping = {
        comment           '借用违规处理'
        dynamicUpdate     true
        id                generator: 'identity', comment: 'ID'
        bookingItem       comment: '教室借用项'
        type              type: 'text', comment: '违规类别'
        content           type: 'text', comment: '违规内容'
        pictures          sqlType: 'text[]', type: PostgreSQLStringArrayUserType, comment: '违规照片'
        week              comment: '周次'
        userCount         comment: '实际人数'
        reporter          comment: '报告人'
        dateCreated       comment: '创建时间'
        dateModified      comment: '修改时间'
        handleOutcome     type: 'text', comment: '处理意见'
        handler           comment: '处理人'
        dateHandled       comment: '处理时间'
        checkOutcome      type: 'text', comment: '学院意见'
        checker           comment: '审核人'
        dateChecked       comment: '审批时间'
        approvalOutcome   type: 'text', comment: '处理决定'
        approver          comment: '审批人'
        dateApproved      comment: '审批时间'
        status            comment: '状态-0:待处理;1:待学院核实;2:学院已处理;3:已核实;4:不违规'
    }

    static constraints = {
        userCount         nullable: true
        handleOutcome     nullable: true
        handler           nullable: true
        dateHandled       nullable: true
        checkOutcome      nullable: true
        checker           nullable: true
        dateChecked       nullable: true
        approvalOutcome   nullable: true
        approver          nullable: true
        dateApproved      nullable: true
    }
}
