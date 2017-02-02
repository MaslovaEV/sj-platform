package com.bwsw.sj.common.config

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals._

object ConfigurationSettingsUtils {
  private val configService = ConnectionRepository.getConfigService

  def createConfigurationSettingName(domain: String, name: String) = {
    domain + "." + name
  }

  def clearConfigurationSettingName(domain: String, name: String) = {
    name.replaceFirst(domain + ".", "")
  }

  def getClientRetryPeriod() = {
    getIntConfigSetting(tgClientRetryPeriodTag)
  }

  def getServerRetryPeriod() = {
    getIntConfigSetting(tgServerRetryPeriodTag)
  }

  def getRetryCount() = {
    getIntConfigSetting(tgRetryCountTag)
  }

  def getGeoIpAsNumFileName() = {
    getStringConfigSetting(geoIpAsNum)
  }

  def getGeoIpAsNumv6FileName() = {
    getStringConfigSetting(geoIpAsNumv6)
  }

  def getKafkaSubscriberTimeout() = {
    getIntConfigSetting(kafkaSubscriberTimeoutTag)
  }

  def getZkSessionTimeout() = {
    getIntConfigSetting(zkSessionTimeoutTag)
  }

  def getFrameworkJarName() = {
    getStringConfigSetting(createConfigurationSettingName(ConfigLiterals.systemDomain, getStringConfigSetting(frameworkTag)))
  }

  def getCrudRestHost() = {
    getStringConfigSetting(hostOfCrudRestTag)
  }

  def getCrudRestPort() = {
    getIntConfigSetting(portOfCrudRestTag)
  }

  def getMarathonConnect() = {
    getStringConfigSetting(marathonTag)
  }

  def getMarathonTimeout() = {
    getIntConfigSetting(marathonTimeoutTag)
  }

  def getFrameworkBackoffSeconds() = {
    getIntConfigSetting(frameworkBackoffSeconds)
  }

  def getFrameworkBackoffFactor() = {
    getDoubleConfigSettings(frameworkBackoffFactor)
  }

  def getFrameworkMaxLaunchDelaySeconds() = {
    getIntConfigSetting(frameworkMaxLaunchDelaySeconds)
  }

  private def getIntConfigSetting(name: String) = {
    getConfigSettings(name).toInt
  }

  private def getStringConfigSetting(name: String) = {
    getConfigSettings(name)
  }

  private def getDoubleConfigSettings(name: String) = {
    getConfigSettings(name).toDouble
  }

  private def getConfigSettings(name: String) = {
    val maybeSetting = configService.get(name)
    if (maybeSetting.isEmpty)
      throw new NoSuchFieldException(s"Config setting is named '$name' has not found")
    else
      maybeSetting.get.value
  }
}
