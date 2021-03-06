package ch.cern.cmms.eamlightweb.workorders.partusage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import ch.cern.cmms.eamlightweb.tools.AuthenticationTools;
import ch.cern.cmms.eamlightweb.tools.WSHubController;
import ch.cern.eam.wshub.core.client.InforClient;
import ch.cern.cmms.eamlightweb.tools.autocomplete.GridUtils;
import ch.cern.cmms.eamlightweb.tools.autocomplete.SimpleGridInput;
import ch.cern.cmms.eamlightweb.tools.autocomplete.WhereParameter;
import ch.cern.cmms.eamlightweb.tools.autocomplete.WhereParameter.OPERATOR;
import ch.cern.cmms.eamlightweb.tools.Pair;
import ch.cern.cmms.eamlightweb.tools.interceptors.RESTLoggingInterceptor;
import ch.cern.cmms.eamlightejb.data.ApplicationData;
import ch.cern.cmms.eamlightejb.workorders.WorkOrderPartUsage;
import ch.cern.eam.wshub.core.services.material.entities.IssueReturnPartTransaction;
import ch.cern.eam.wshub.core.services.material.entities.IssueReturnPartTransactionLine;
import ch.cern.eam.wshub.core.services.material.entities.IssueReturnPartTransactionType;
import ch.cern.eam.wshub.core.services.grids.entities.GridRequestCell;
import ch.cern.eam.wshub.core.services.grids.entities.GridRequestResult;
import ch.cern.eam.wshub.core.services.grids.entities.GridRequestRow;
import ch.cern.eam.wshub.core.tools.InforException;
import ch.cern.eam.wshub.core.services.workorders.entities.Activity;
import ch.cern.eam.wshub.core.services.workorders.entities.WorkOrder;


@Path("/partusage")
@RequestScoped
@Interceptors({ RESTLoggingInterceptor.class })
public class PartUsageRest extends WSHubController {

	@Inject
	private InforClient inforClient;
	@Inject
	private GridUtils gridUtils;
	@Inject
	private ApplicationData applicationData;
	@Inject
	private AuthenticationTools authenticationTools;

	@GET
	@Path("/stores")
	@Produces("application/json")
	@Consumes("application/json")
	public Response loadStoreList() {
		try {
			// Init the list
			List<Pair> stores = new ArrayList<>();
			// Prepare the input
			SimpleGridInput input = new SimpleGridInput("182", "LVIRSTOR", "187");
			input.setRowCount("1000");
			input.getInforParams().put("storefield", applicationData.getStoreField());
			input.getInforParams().put("userfunction", applicationData.getUserFunction());
			input.getWhereParams().put("storecode", new WhereParameter("")); // All
			input.getSortParams().put("storecode", true); // true=ASC,
			input.setFields(Arrays.asList("682", "133")); // 682=storecode,
															// 133=des_text
			// Search for the results
			GridRequestResult res = gridUtils.getGridRequestResult(input, authenticationTools.getInforContext());
			// Create the list of results
			List<List<String>> gridRow = Arrays.stream(res.getRows())
					.map(row -> Arrays.stream(row.getCell()).filter(cell -> input.getFields().contains(cell.getCol()))
							.sorted((cell1, cell2) -> input.getFields().indexOf(cell1.getCol())
									- input.getFields().indexOf(cell2.getCol()))
							.map(cell -> cell.getContent()).collect(Collectors.toList()))
					.collect(Collectors.toList());

			if (!gridRow.isEmpty()) {
				for (List<String> row : gridRow) {
					stores.add(new Pair(row.get(0)// storecode
							, row.get(1)// des_text
					));
				}
			}
			return ok(stores);
		} catch (InforException e) {
			return badRequest(e);
		} catch(Exception e) {
			return serverError(e);
		}
	}


	@GET
	@Path("/bins")
	@Produces("application/json")
	@Consumes("application/json")
	public Response loadBinList(@QueryParam("transaction") String transaction, @QueryParam("bin") String bin,
			@QueryParam("part") String part, @QueryParam("store") String store) {
		try {
			SimpleGridInput input;
			if (transaction.startsWith("I")) {// ISSUE
				input = new SimpleGridInput("183", "LVISSUEBIN", "188");
				if (bin != null && !bin.isEmpty())
					input.getWhereParams().put("bincode", new WhereParameter(bin));
			} else // RETURN
				input = new SimpleGridInput("191", "LVRETURNBIN", "196");
			input.setRowCount("1000");
			input.getInforParams().put("part_code", part);
			input.getInforParams().put("part_org", applicationData.getControlOrg());
			input.getInforParams().put("store_code", store);
			input.setFields(Arrays.asList("830", "824")); // 830=bincode,
															// 824=bindescription
			GridRequestResult res = gridUtils.getGridRequestResult(input, authenticationTools.getInforContext());

			final String db = (bin == null || "".equals(bin)) ? loadDefaultBin(part, store, transaction) : ""; // default
			// bin
			List<List<String>> gridRow = Arrays.stream(res.getRows())
					.map(row -> Arrays.stream(row.getCell()).filter(cell -> input.getFields().contains(cell.getCol()))
							.sorted((cell1, cell2) -> input.getFields().indexOf(cell1.getCol())
									- input.getFields().indexOf(cell2.getCol()))
							.map(cell -> cell.getContent()).collect(Collectors.toList()))
					.collect(Collectors.toList());

			if (!gridRow.isEmpty()) {
				List<Pair> orderedListWithDefault = Arrays
						.stream(gridRow.stream().map(l -> l.stream().toArray(String[]::new)).toArray(String[][]::new))
						.sorted((cell1,
								cell2) -> (db.equals(cell1[0]) ? -1
										: db.equals(cell2[0]) ? 1 : (cell1[0].compareTo(cell2[0]))))
						.map(rowBin -> new Pair(rowBin[0], // bincode
								rowBin[1] // bindescription
						)).collect(Collectors.toList());
				return ok(orderedListWithDefault);
			} else {
				return ok(new ArrayList<Pair>());
			}
		} catch (InforException e) {
			return badRequest(e);
		} catch(Exception e) {
			return serverError(e);
		}
	}


	/**
	 * Loads the default bin
	 * 
	 * @return Default bin
	 * @throws InforException
	 *             Error loading the default bin
	 */
	private String loadDefaultBin(String part, String store, String transaction) throws InforException {
		String result = null;
		SimpleGridInput input = new SimpleGridInput("110", "SSPART_STO", "133");
		input.setGridType("LIST");
		input.getInforParams().put("partcode", part);
		input.getInforParams().put("partorg", applicationData.getControlOrg());
		input.getInforParams().put("userfunction", "SSPART");
		input.getWhereParams().put("storecode", new WhereParameter(OPERATOR.EQUALS, store));
		if (transaction.startsWith("I")) // ISSUE
			input.setFields(Arrays.asList("661")); // 661=defaultbin
		else
			input.setFields(Arrays.asList("12651"));// 12651=defaultreturnbin

		GridRequestResult res = gridUtils.getGridRequestResult(input, authenticationTools.getInforContext());

		List<List<String>> gridRow = Arrays.stream(res.getRows())
				.map(row -> Arrays.stream(row.getCell()).filter(cell -> input.getFields().contains(cell.getCol()))
						.sorted((cell1, cell2) -> input.getFields().indexOf(cell1.getCol())
								- input.getFields().indexOf(cell2.getCol()))
						.map(cell -> cell.getContent()).collect(Collectors.toList()))
				.collect(Collectors.toList());

		try {
			if (gridRow != null && !gridRow.isEmpty() && gridRow.get(0) != null && !gridRow.get(0).isEmpty())
				result = gridRow.get(0).get(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result == null ? "" : result;
	}

	@POST
	@Path("/transaction")
	@Produces("application/json")
	@Consumes("application/json")
	public Response createPartUsage(IssueReturnPartTransaction transaction) {
		try {
			transaction.setTransactionOn(IssueReturnPartTransactionType.WORKORDER);
			return ok(inforClient.getPartMiscService().createIssueReturnTransaction(authenticationTools.getInforContext(), transaction));
		} catch (InforException e) {
			return badRequest(e);
		} catch(Exception e) {
			return serverError(e);
		}
	}


	@POST
	@Path("/init")
	@Produces("application/json")
	@Consumes("application/json")
	public Response initPartUsage(WorkOrder workOrder) {
		try {
			// Create the issue/return transacction for the workOrder
			IssueReturnPartTransaction transaction = createTransaction(workOrder);
			// Create the transaction line and add it to the transaction
			IssueReturnPartTransactionLine transLine = createTransactionLine();
			// Add the line to the transaction
			transaction.getTransactionlines().add(transLine);
			return ok(transaction);
		} catch (InforException e) {
			return badRequest(e);
		} catch(Exception e) {
			return serverError(e);
		}
	}


	/**
	 * Creates the transaction line that is going to be added to the transaction
	 * 
	 * @return The transaction line to be created
	 */
	private IssueReturnPartTransactionLine createTransactionLine() {
		// Init the line
		IssueReturnPartTransactionLine line = new IssueReturnPartTransactionLine();
		/* Assign attributes */
		// Part Code
		line.setPartCode(null);
		// Part Org
		line.setPartOrg("*");
		// Bin
		line.setBin(null);
		// Lot
		line.setLot(null);
		// Quantity
		line.setTransactionQty("1");
		// Asset code
		line.setAssetIDCode(null);
		// Returns line created
		return line;
	}

	/**
	 * Creates the transaction
	 * 
	 * @return
	 * @throws InforException
	 */
	private IssueReturnPartTransaction createTransaction(WorkOrder workOrder) throws InforException {
		// Create the transaction
		IssueReturnPartTransaction transaction = new IssueReturnPartTransaction();
		/* Asign attributes */
		// Transaction Type
		transaction.setTransactionOn(IssueReturnPartTransactionType.WORKORDER);
		// Work order number
		transaction.setWorkOrderNumber(workOrder.getNumber());
		// Equipment
		transaction.setEquipmentCode(workOrder.getEquipmentCode());
		// Activity
		// Read activities
		Activity[] activities = inforClient.getLaborBookingService().readActivities(authenticationTools.getInforContext(), workOrder.getNumber());
		if (activities != null && activities.length == 1) {
			transaction.setActivityCode(activities[0].getActivityCode());
		} else {
			transaction.setActivityCode(null);
		}
		// Store
		transaction.setStoreCode(null);
		// Department
		transaction.setDepartmentCode(workOrder.getDepartmentCode());
		// Transaction type
		transaction.setTransactionType("ISSUE");
		// Init lines
		transaction.setTransactionlines(new LinkedList<IssueReturnPartTransactionLine>());
		// Returns the transaction created
		return transaction;
	}

	@GET
	@Path("/transactions/{workorder}")
	@Produces("application/json")
	@Consumes("application/json")

	public Response loadPartUsageList(@PathParam("workorder") String workorder) {
		// 110 partorganization // 128 dbactivity // 315 workordernum // 317
		// matlist_lineno // 318 partcode // 319 storecode // 357 partuom
		// 519 equipmentorganization // 521 assetidorganization // 988
		// plannedqty // 989 source // 990 reservedqty // 991 allocatedqty
		// 994 partdescription // 6996 stock // 8775 preventreorders // 9575
		// manufactpart // 9576 manufacturer // 9587 usedqty
		// 15666 parentpart // 15669 conditioncode // 16027 trackbycondition //
		// 16460 longdescription // 17473 activity
		// 17474 job // 17484 activity_display // 17485 job_display
		try {
			// Init the list
			List<WorkOrderPartUsage> partUsageList = new ArrayList<>();
			// Just execute if there is work order
			if (workorder != null) {
				// Creates simple grid input
				SimpleGridInput input = new SimpleGridInput("226", "WSJOBS_PAR", "237");
				// GridController Type
				input.setGridType("LIST");
				// Fields to be retrieved
				input.setFields(Arrays.asList("128", "318", "357", "994", "17473", "357", "9587", "17484", "319"));
				// Rows to print
				input.setRowCount("1000");
				Map<String, String> inforParams = new HashMap<>();
				inforParams.put("workordernum", workorder);
				inforParams.put("headeractivity", "0");
				inforParams.put("headerjob", "0");
				// Add infor params
				inforParams.forEach((k, v) -> {
					input.getInforParams().put(k, v);
				});
				// Execute grid
				GridRequestResult res = gridUtils.getGridRequestResult(input, authenticationTools.getInforContext());
				// Process result
				Arrays.stream(res.getRows()).forEach(row -> processRow(row, input.getFields(), partUsageList));
			}
			return ok(partUsageList);
		} catch (InforException e) {
			return badRequest(e);
		} catch(Exception e) {
			return serverError(e);
		}
	}


	private boolean processRow(GridRequestRow row, List<String> fields, List<WorkOrderPartUsage> partUsageList) {
		// Create new Work Order part usage
		WorkOrderPartUsage partUsage = new WorkOrderPartUsage();
		Arrays.stream(row.getCell()).filter(cell -> fields.contains(cell.getCol()))
				.forEach(cell -> addPropertyToPartUsage(partUsage, cell));
		// Add element to the list
		partUsageList.add(partUsage);
		// Row processed
		return true;
	}

	private boolean addPropertyToPartUsage(WorkOrderPartUsage partUsage, GridRequestCell cell) {
		// Check property
		switch (cell.getCol()) {
		case "318":/* Part code */
			partUsage.setPartCode(cell.getContent());
			break;
		case "357":/* Part partuom */
			partUsage.setPartUom(cell.getContent());
			break;
		case "994":/* Part description */
			partUsage.setPartDesc(cell.getContent());
			break;
		case "17484":/* Activity display */
			partUsage.setActivity(cell.getContent());
			break;
		case "319": /* Store code */
			partUsage.setStoreCode(cell.getContent());
			break;
		case "9587":/* Used quantity */
			partUsage.setQuantity(cell.getContent());
			// Set also the transaction type
			try {
				if (Integer.valueOf(partUsage.getQuantity()) < 0) {
					// Set return
					partUsage.setTransType("Return");
					partUsage.setQuantity("" + (-1 * Integer.valueOf(partUsage.getQuantity())));
				} else {/* Issue */
					partUsage.setTransType("Issue");
				}
			} catch (Exception e) {/* Error parsing */
				// Set issue
				partUsage.setTransType("Issue");
			}
			break;
		}
		// Property added
		return true;
	}

}
