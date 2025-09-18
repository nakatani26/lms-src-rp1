package jp.co.sss.lms.form;

import java.util.LinkedHashMap;
import java.util.List;

import lombok.Data;

/**
 * 勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class AttendanceForm {

	/** LMSユーザーID */
	private Integer lmsUserId;
	/** グループID */
	private Integer groupId;
	/** 年間計画No */
	private String nenkanKeikakuNo;
	/** ユーザー名 */
	private String userName;
	/** 退校フラグ */
	private Integer leaveFlg;
	/** 退校日 */
	private String leaveDate;
	/** 退校日（表示用） */
	private String dispLeaveDate;
	/** 中抜け時間(プルダウン) */
	private LinkedHashMap<Integer, String> blankTimes;
	/** 日次の勤怠フォームリスト */
	private List<DailyAttendanceForm> attendanceList;
	/** 出勤・退勤時間(時プルダウン) */
	private LinkedHashMap<Integer, String> timeHours;
	/** 出勤・退勤時間(分プルダウン) */
	private LinkedHashMap<Integer, String> timeMinutes;
//	/** 出勤時間（時） */
//	private Integer trainingStartTimeHour;
//	/** 出勤時間（分） */
//	private Integer trainingStartTimeMinute;
//	/** 出勤・退勤時間(時プルダウン) */
//	private LinkedHashMap<Integer, String> trainingEndTimeHour;
//	/** 出勤・退勤時間(分プルダウン) */
//	private LinkedHashMap<Integer, String> trainingEndTimeMinute;
	
}
