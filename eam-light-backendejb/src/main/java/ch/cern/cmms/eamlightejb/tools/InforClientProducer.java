package ch.cern.cmms.eamlightejb.tools;

import ch.cern.cmms.eamlightejb.data.ApplicationData;
import ch.cern.eam.wshub.core.client.InforClient;
import ch.cern.eam.wshub.core.interceptors.InforInterceptor;
import ch.cern.eamtools.core.soaphandler.SOAPHandlerResolver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.sql.DataSource;

@ApplicationScoped
public class InforClientProducer {

	@Produces
	private InforClient inforClient;
	@Resource
	private DataSource datasource;
	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;
	@Resource
	private ManagedExecutorService executorService;
	@Inject
	private InforInterceptor inforInterceptor;
	@Inject
	private ApplicationData applicationData;

	@PostConstruct
	public void init() {
			inforClient = new InforClient.Builder(applicationData.getInforWSURL(), applicationData.getTenant())
					.withDefaultOrganizationCode("*")
					.withSOAPHandlerResolver(new SOAPHandlerResolver())
					.withDataSource(datasource)
					.withEntityManagerFactory(entityManagerFactory)
					.withExecutorService(executorService)
					.withInforInterceptor(inforInterceptor)
					.build();

	}
	
}