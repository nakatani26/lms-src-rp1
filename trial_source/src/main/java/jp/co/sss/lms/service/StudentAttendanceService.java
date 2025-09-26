package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		attendanceForm.setTimeHours(attendanceUtil.setTrainingStartTimeHour());
		attendanceForm.setTimeMinutes(attendanceUtil.setTrainingStartTimeMinute());


		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			//↓Task26 出勤時間を分割してセット
			if (attendanceManagementDto.getTrainingStartTime() != null && attendanceManagementDto.getTrainingStartTime().contains(":")) {
			    String[] startParts = attendanceManagementDto.getTrainingStartTime().split(":");
			    dailyAttendanceForm.setTrainingStartTimeHour(Integer.parseInt(startParts[0]));
			    dailyAttendanceForm.setTrainingStartTimeMinute(Integer.parseInt(startParts[1]));
			}

			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			//↓Task26 退勤時間を分割してセット
			if (attendanceManagementDto.getTrainingEndTime() != null && attendanceManagementDto.getTrainingEndTime().contains(":")) {
			    String[] endParts = attendanceManagementDto.getTrainingEndTime().split(":");
			    dailyAttendanceForm.setTrainingEndTimeHour(Integer.parseInt(endParts[0]));
			    dailyAttendanceForm.setTrainingEndTimeMinute(Integer.parseInt(endParts[1]));
			}

			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			
			
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ          元の状態
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

	    Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
	            : attendanceForm.getLmsUserId();

	    // 現在の勤怠情報リストを取得
	    List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
	            .findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

	    // 更新日
	    Date now = new Date();

	    for (DailyAttendanceForm dailyForm : attendanceForm.getAttendanceList()) {

	        // 出勤時間を文字列に変換
	        if (dailyForm.getTrainingStartTimeHour() != null && dailyForm.getTrainingStartTimeMinute() != null) {
	            dailyForm.setTrainingStartTime(
	                String.format("%02d:%02d", dailyForm.getTrainingStartTimeHour(), dailyForm.getTrainingStartTimeMinute())
	            );
	        } else {
	            dailyForm.setTrainingStartTime("");
	        }

	        // 退勤時間を文字列に変換
	        if (dailyForm.getTrainingEndTimeHour() != null && dailyForm.getTrainingEndTimeMinute() != null) {
	            dailyForm.setTrainingEndTime(
	                String.format("%02d:%02d", dailyForm.getTrainingEndTimeHour(), dailyForm.getTrainingEndTimeMinute())
	            );
	        } else {
	            dailyForm.setTrainingEndTime("");
	        }

	        // 該当する既存データを探す（同じ trainingDate）
	        Date formDate = dateUtil.parse(dailyForm.getTrainingDate());
	        TStudentAttendance existing = null;
	        for (TStudentAttendance entity : tStudentAttendanceList) {
	            if (entity.getTrainingDate().equals(formDate)) {
	                existing = entity;
	                break;
	            }
	        }

	        TStudentAttendance attendance;
	        if (existing != null) {
	            // 更新処理
	            attendance = existing;
	        } else {
	            // 新規作成
	            attendance = new TStudentAttendance();
	            attendance.setTrainingDate(formDate);
	            attendance.setFirstCreateUser(loginUserDto.getLmsUserId());
	            attendance.setFirstCreateDate(now);
	            tStudentAttendanceList.add(attendance);  // 新規のみ追加
	        }

	        // 共通情報の設定
	        attendance.setLmsUserId(lmsUserId);
	        attendance.setAccountId(loginUserDto.getAccountId());
	        attendance.setDeleteFlg(Constants.DB_FLG_FALSE);
	        attendance.setLastModifiedUser(loginUserDto.getLmsUserId());
	        attendance.setLastModifiedDate(now);

	        // 時刻を整形してセット
	        TrainingTime startTime = null;
	        TrainingTime endTime = null;
	        try {
	            if (!dailyForm.getTrainingStartTime().isEmpty()) {
	                startTime = new TrainingTime(dailyForm.getTrainingStartTime());
	                attendance.setTrainingStartTime(startTime.getFormattedString());
	            } else {
	                attendance.setTrainingStartTime("");
	            }

	            if (!dailyForm.getTrainingEndTime().isEmpty()) {
	                endTime = new TrainingTime(dailyForm.getTrainingEndTime());
	                attendance.setTrainingEndTime(endTime.getFormattedString());
	            } else {
	                attendance.setTrainingEndTime("");
	            }
	        } catch (Exception e) {
	            // 時刻のパースエラーはスルー（ログに出した方がベター）
	        }

	        // 中抜け時間
	        attendance.setBlankTime(dailyForm.getBlankTime());

	        // 備考
	        attendance.setNote(dailyForm.getNote());

	        // ステータス（遅刻、早退など）
	        if ((startTime != null || endTime != null)
	                && !"欠席".equals(dailyForm.getStatusDispName())) {
	            AttendanceStatusEnum statusEnum = attendanceUtil.getStatus(startTime, endTime);
	            attendance.setStatus(statusEnum.code);
	        }
	    }

	    // 保存処理（IDで insert / update を分岐）
	    for (TStudentAttendance attendance : tStudentAttendanceList) {
	        if (attendance.getStudentAttendanceId() == null) {
	            tStudentAttendanceMapper.insert(attendance);
	        } else {
	            tStudentAttendanceMapper.update(attendance);
	        }
	    }

	    return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	
	/**
	 * 勤怠情報（受講生入力）未入力件数取得
	 * 引数に基づいて、未入力かをtrue,falseで返す
	 * @param lmsUserId
	 * @param deleteFlg
	 * @param date
	 * @return
	 * @author 中谷文乃_Task25
	 */
	public boolean notEnterCount(Integer lmsUserId,
			short deleteFlg, String trainingDate) {

		/**勤怠情報（受講生入力）未入力件数取得 
		 * tStudentAttendanceMapper.getAttendanceNoInputを呼んで、未入力の勤怠をDBから取得
		 * cntに結果を格納
		 */
		Integer COUNT = tStudentAttendanceMapper.notEnterCount(lmsUserId, trainingDate, deleteFlg);
		
		// 件数が1件以上の場合
		if (COUNT > 0) {
			return true;
		} else {
			return false;
		}

	}

}
