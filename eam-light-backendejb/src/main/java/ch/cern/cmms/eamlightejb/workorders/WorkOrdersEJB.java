package ch.cern.cmms.eamlightejb.workorders;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import ch.cern.cmms.eamlightejb.UserTools;
import ch.cern.eam.wshub.core.client.InforContext;
import org.jboss.logging.Logger.Level;

import ch.cern.cmms.eamlightejb.tools.LoggingService;
import ch.cern.eam.wshub.core.tools.InforException;

/**
 * Session Bean implementation class WorkOrdersEJB
 */
@Stateless
@LocalBean
public class WorkOrdersEJB {

	@PersistenceContext
	private EntityManager em;

	@Inject
	private LoggingService logger;

	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;

	@EJB
	private UserTools userTools;
	/**
	 * Default constructor.
	 */
	public WorkOrdersEJB() {
		// TODO Auto-generated constructor stub
	}

	//
	//
	//
	public List<MyWorkOrder> getWorkOrders(String code, int maxResults) {
		return em.createNamedQuery(MyWorkOrder.GET_WOS, MyWorkOrder.class).setParameter("codeParam", code)
				.setMaxResults(maxResults).getResultList();
	}

	//
	//
	//
	public Person getPerson(String code) {
		try {
			return em.createNamedQuery(Person.PERSON_GETPERSON, Person.class).setParameter("codeParam", code)
					.getSingleResult();
		} catch (javax.persistence.NoResultException e) {
			return null;
		}
	}

	public List<Person> getPersons(String code) {
		return em.createNamedQuery(Person.PERSON_GETPERSONS, Person.class).setParameter("codeParam", code)
				.setMaxResults(10).getResultList();
	}

	public List<Type> getTypes(String query, String code) {
		List<Type> types = em.createNamedQuery(query, Type.class).setParameter("codeParam", code).getResultList();
		return types;
	}

	public List<MyWorkOrder> getWOs(InforContext inforContext) throws InforException {
		String username = inforContext.getCredentials().getUsername();
		List<MyWorkOrder> types = em.createNamedQuery(MyWorkOrder.GET_MY_OPEN_WOS, MyWorkOrder.class)
				.setParameter("user", username).getResultList();
		return types;
	}

	public List<MyWorkOrder> getTeamWOs(InforContext inforContext) {
		// Just execute if there are departments
		// TODO
		/*
		if (eamAccount.getUserDepartments() == null || eamAccount.getUserDepartments().isEmpty())
			return new ArrayList<>();
		List<MyWorkOrder> types = em.createNamedQuery(MyWorkOrder.GET_MY_TEAMS_WOS, MyWorkOrder.class)
				.setParameter("user", eamAccount.getUserCode())
				.setParameter("departments", eamAccount.getUserDepartments()).getResultList();
		return types;
		*/
		return new ArrayList<>();
	}

	public List<WorkOrderType> getWorkOrderTypes(InforContext inforContext, String status, String oldType, boolean newWorkOrder,
			boolean isPMWorkOrder) throws InforException {
		String group = userTools.getUserGroup(inforContext);
		List<WorkOrderType> types = new ArrayList<>();
		if (newWorkOrder) {
			types = em.createNamedQuery(WorkOrderType.GET_TYPES_FOR_NEW_WO, WorkOrderType.class)
					.setParameter("group", group).getResultList();
		} else {
			if (!isPMWorkOrder)
				types = em.createNamedQuery(WorkOrderType.GET_TYPES_FOR_EXISTING_WO, WorkOrderType.class)
						.setParameter("status", status).setParameter("userGroup", group)
						.setParameter("oldType", oldType).getResultList();
			else
				types = em.createNamedQuery(WorkOrderType.GET_TYPES_FOR_EXISTING_WO_PPM, WorkOrderType.class)
						.setParameter("status", status).setParameter("userGroup", group)
						.setParameter("oldType", oldType).getResultList();
		}
		return types;
	}

	public List<WorkOrderStatus> getWorkOrderStatuses(InforContext inforContext, String status, String jobType,
			boolean newWorkOrder) throws InforException {
		String user = inforContext.getCredentials().getUsername();
		String group = userTools.getUserGroup(inforContext);
		List<WorkOrderStatus> statuses;
		if (newWorkOrder) {
			// Check type
			jobType = jobType == null || "".equals(jobType) ? "CD" : jobType;
			statuses = em.createNamedQuery(WorkOrderStatus.GET_STATUSES_FOR_NEW_WO, WorkOrderStatus.class)
					.setParameter("group", group).setParameter("user", user).setParameter("jobType", jobType)
					.getResultList();
		} else {
			statuses = em.createNamedQuery(WorkOrderStatus.GET_STATUSES_FOR_EXISTING_WO, WorkOrderStatus.class)
					.setParameter("status", status).setParameter("group", group).setParameter("user", user)
					.getResultList();
		}
		return statuses;
	}

	//
	// GET WORK ORDERS ASSOCIATED TO AND OBJECT (ASSET,POSITION,SYSTEM)
	//
	public List<MyWorkOrder> getObjectWorkOrders(String objectCode) {
		return em.createNamedQuery(MyWorkOrder.GET_OBJWOS, MyWorkOrder.class).setParameter("objectCode", objectCode)
				.getResultList();
	}

	//
	// GET HISTORY OF ASSET,POSITION,SYSTEM
	//
	public List<EquipmentHistory> getObjectHistory(String objectCode) {
		return em.createNamedQuery(EquipmentHistory.GET_OBJHISTORY, EquipmentHistory.class).setParameter("objectCode", objectCode)
				.getResultList();
	}

	//
	// GET WORK ORDER EQUIPMENT
	//
	public List<WorkOrderEquipment> getWorkOrderEquipment(String wonumber) {
		return em.createNamedQuery(WorkOrderEquipment.GET_WO_EQUIPMENT, WorkOrderEquipment.class)
				.setParameter("wonumber", wonumber).getResultList();
	}

	//
	// GET WORK ORDER STATUS and TYPE
	//
	private static String SELECT_EQ_CLASS = " select obj_class from R5EVENTS left join R5OBJECTS on evt_object = obj_code where evt_code = :woid ";

	public String getEquipmentClass(String woid) throws InforException {
		Object[] results = null;
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			results = (Object[]) em.createNativeQuery(SELECT_EQ_CLASS).setParameter("woid", woid).getSingleResult();
		} catch (Exception exception) {
			logger.log(Level.ERROR, exception.getMessage());
		} finally {
			em.close();
		}

		return (results != null && results.length > 0 && results[0] != null) ? results[0].toString() : null;
	}

}
