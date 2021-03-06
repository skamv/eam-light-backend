package ch.cern.cmms.eamlightejb.data;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.xml.bind.annotation.XmlTransient;

import ch.cern.cmms.eamtools.config.ApplicationDataReader;

@Dependent
public class ApplicationData  {

	private Map<String, String> eamlightValues;
	private Map<String, String> kioskValues;

	@Inject
	private ApplicationDataReader configReader;

	@PostConstruct
	private void init() {
		eamlightValues = configReader.getProperties("EAMLIGHT");
		kioskValues = configReader.getProperties("KIOSK");
	}

	//
	// Getters for individual properties
	//
	public String getTenant() {
		return getVariableValue("INFOR_TENANT");
	}

	public String getOrganization() {
		return getVariableValue("INFOR_ORGANIZATION");
	}

	public String getInforWSURL() {
		return getVariableValue("INFOR_WS_URL");
	}

	@XmlTransient
	public String getPassphrase() {
		return getVariableValue("EAMLIGHT_PASSPHRASE");
	}

	/************************
	 * KIOSK PROPERTIES
	 *
	 ************************/

	public String getUserFunction() {
		return kioskValues.get("USER_FUNCTION");
	}

	public String getStoreField() {
		return kioskValues.get("STORE_FIELD");
	}

	public String getControlOrg() {
		return kioskValues.get("ORGANIZATION_CODE");
	}

	public String getMultiEquipmentWO() {
		return kioskValues.get("MULTI_EQUIPMENT_WO").toLowerCase();
	}

	public String getPartForAssetDataspyID() {
		return kioskValues.get("PART_FOR_ASSET_DATASPYID");
	}


	/************************
	 * LINKS TO EXTENDED
	 *
	 ************************/

	public String getExtendedWOLink() {
		return eamlightValues.get("EXT_WOLINK");
	}

	public String getExtendedAssetLink() {
		return eamlightValues.get("EXT_ASSETLINK");
	}

	public String getExtendedPositionLink() {
		return eamlightValues.get("EXT_POSITIONLINK");
	}

	public String getExtendedSystemLink() {
		return eamlightValues.get("EXT_SYSTEMLINK");
	}

	public String getExtendedPartLink() {
		return eamlightValues.get("EXT_PARTLINK");
	}

	/************************
	 * LINKS TO EAM INTEGRATIONS
	 *
	 ************************/

	public String getLinkToEAMIntegration() {
		return eamlightValues.get("EXT_EAMINTEG");
	}

	public String getEamlightOldURL() {
		return eamlightValues.get("EAMLIGHT_OLD_URL");
	}

	public String getServiceNowURL() {
		return eamlightValues.get("SERVICE_NOW_URL");
	}

	public String getPrintingLinkToAIS() {
		return eamlightValues.get("AISBI_PRINT");
	}

	public String getPrintingChecklistLinkToAIS() {
		return eamlightValues.get("AISBI_PRINT_CHECKLIST");
	}

	public String getGISProcedureLinkWO() {
		return eamlightValues.get("GIS_PROCEDURE_LINK_WO");
	}

	public String getGISProcedureLinkEQP() {
		return eamlightValues.get("GIS_PROCEDURE_LINK_EQP");
	}

	public ApplicationData copy() {
		ApplicationData applicationData = new ApplicationData();
		return applicationData;
	}

	private String getVariableValue(String variableName) {
		String valueFromEnv = System.getenv().get(variableName);
		if (valueFromEnv != null && !valueFromEnv.isEmpty()) {
			return valueFromEnv;
		} else {
			return null;
		}
	}

}
