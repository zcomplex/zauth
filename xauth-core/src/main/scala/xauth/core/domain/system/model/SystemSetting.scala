package xauth.core.domain.system.model

import xauth.util.{EnumVal, EnumFromVal}

case class SystemSetting(key: SettingKey, value: String)

enum SettingKey(val value: String) extends EnumVal[String]:
  case Init extends SettingKey("sys.init")
  
  def settingWith(v: String): SystemSetting = SystemSetting(this, v)

object SettingKey extends EnumFromVal[SettingKey, String]