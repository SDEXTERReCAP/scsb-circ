package org.recap.request;

import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCase;
import org.recap.ReCAPConstants;
import org.recap.controller.RequestItemValidatorController;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.*;
import org.recap.repository.InstitutionDetailsRepository;
import org.recap.repository.ItemDetailsRepository;
import org.recap.repository.RequestTypeDetailsRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by hemalathas on 17/2/17.
 */
public class ItemEDDRequestServiceUT extends BaseTestCase{

    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    @Mock
    ItemEDDRequestService itemEDDRequestService;

    @Mock
    Exchange exchange;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    InstitutionDetailsRepository institutionDetailRepository;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    private RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Mock
    private RequestItemValidatorController requestItemValidatorController;

    @Mock
    private ItemRequestService itemRequestService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testEddRequestItem() throws Exception {
        ResponseEntity res = new ResponseEntity(ReCAPConstants.WRONG_ITEM_BARCODE,HttpStatus.CONTINUE);
        ItemRequestInformation itemRequestInfo = new ItemRequestInformation();
        itemRequestInfo.setItemBarcodes(Arrays.asList("23"));
        itemRequestInfo.setItemOwningInstitution("PUL");
        itemRequestInfo.setExpirationDate(new Date().toString());
        itemRequestInfo.setRequestingInstitution("PUL");
        itemRequestInfo.setTitleIdentifier("titleIdentifier");
        itemRequestInfo.setPatronBarcode("4356234");
        itemRequestInfo.setBibId("1");
        itemRequestInfo.setDeliveryLocation("PB");
        itemRequestInfo.setEmailAddress("hemalatha.s@htcindia.com");
        itemRequestInfo.setRequestType("Recall");
        itemRequestInfo.setRequestNotes("Test");

        ItemInformationResponse itemResponseInformation1 = new ItemInformationResponse();
        itemResponseInformation1.setSuccess(true);

        ItemInformationResponse itemResponseInformation = new ItemInformationResponse();

        BibliographicEntity bibliographicEntity = saveBibSingleHoldingsSingleItem();
        ItemEntity itemEntity = bibliographicEntity.getItemEntities().get(0);
        Mockito.when(itemEDDRequestService.getLogger()).thenReturn(logger);
        Mockito.when(itemEDDRequestService.getItemRequestService()).thenReturn(itemRequestService);
        Mockito.when(itemEDDRequestService.getItemRequestService().getLogger()).thenReturn(logger);
        Mockito.when(itemEDDRequestService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemEDDRequestService.getRequestItemValidatorController()).thenReturn(requestItemValidatorController);
        Mockito.when(itemEDDRequestService.getRequestTypeDetailsRepository()).thenReturn(requestTypeDetailsRepository);
        Mockito.when(itemEDDRequestService.getItemDetailsRepository()).thenReturn(itemDetailsRepository);
        Mockito.when(itemEDDRequestService.getRequestItemValidatorController().validateItemRequestInformations(itemRequestInfo)).thenReturn(res);
        Mockito.when(itemEDDRequestService.getItemRequestService().searchRecords(itemEntity)).thenReturn(getSearchResultRowList());
        Mockito.when(itemEDDRequestService.getItemRequestService().getTitle(itemRequestInfo.getTitleIdentifier(), itemEntity)).thenCallRealMethod();
        Mockito.when(itemEDDRequestService.getItemDetailsRepository().findByBarcodeIn(itemRequestInfo.getItemBarcodes())).thenReturn(bibliographicEntity.getItemEntities());
        Mockito.when(itemEDDRequestService.eddRequestItem(itemRequestInfo,exchange)).thenCallRealMethod();
        Mockito.when(itemEDDRequestService.getItemRequestService().updateGFA(itemRequestInfo, itemResponseInformation)).thenReturn(itemResponseInformation1);
        ItemInformationResponse itemInfoResponse = itemEDDRequestService.eddRequestItem(itemRequestInfo,exchange);
        assertNotNull(itemInfoResponse);
        assertEquals(itemInfoResponse.getScreenMessage(),"Item Barcode(s) not available in database.");
    }

    public BibliographicEntity saveBibSingleHoldingsSingleItem() throws Exception {

        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setInstitutionCode("UC");
        institutionEntity.setInstitutionName("University of Chicago");
        InstitutionEntity entity = institutionDetailRepository.save(institutionEntity);
        assertNotNull(entity);

        Random random = new Random();
        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        bibliographicEntity.setContent("mock Content".getBytes());
        bibliographicEntity.setCreatedDate(new Date());
        bibliographicEntity.setLastUpdatedDate(new Date());
        bibliographicEntity.setCreatedBy("tst");
        bibliographicEntity.setLastUpdatedBy("tst");
        bibliographicEntity.setOwningInstitutionId(entity.getInstitutionId());
        bibliographicEntity.setOwningInstitutionBibId(String.valueOf(random.nextInt()));
        HoldingsEntity holdingsEntity = new HoldingsEntity();
        holdingsEntity.setContent("mock holdings".getBytes());
        holdingsEntity.setCreatedDate(new Date());
        holdingsEntity.setLastUpdatedDate(new Date());
        holdingsEntity.setCreatedBy("tst");
        holdingsEntity.setLastUpdatedBy("tst");
        holdingsEntity.setOwningInstitutionId(1);
        holdingsEntity.setOwningInstitutionHoldingsId(String.valueOf(random.nextInt()));

        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setOwningInstitutionItemId(String.valueOf(random.nextInt()));
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setBarcode("123");
        itemEntity.setCallNumber("x.12321");
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCallNumberType("1");
        itemEntity.setCustomerCode("123");
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("tst");
        itemEntity.setLastUpdatedBy("tst");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));

        bibliographicEntity.setHoldingsEntities(Arrays.asList(holdingsEntity));
        bibliographicEntity.setItemEntities(Arrays.asList(itemEntity));

        BibliographicEntity savedBibliographicEntity = bibliographicDetailsRepository.saveAndFlush(bibliographicEntity);
        entityManager.refresh(savedBibliographicEntity);
        return savedBibliographicEntity;

    }

    public List<SearchResultRow> getSearchResultRowList(){
        List<SearchItemResultRow> searchItemResultRowList = new ArrayList<>();
        SearchItemResultRow searchItemResultRow = new SearchItemResultRow();
        searchItemResultRow.setCallNumber("x");
        searchItemResultRow.setChronologyAndEnum("test");
        searchItemResultRow.setCustomerCode("PB");
        searchItemResultRow.setBarcode("123");
        searchItemResultRow.setAvailability("Available");
        searchItemResultRow.setCollectionGroupDesignation("Shared");
        searchItemResultRow.setItemId(1);
        searchItemResultRow.setSelectedItem(true);
        searchItemResultRowList.add(searchItemResultRow);
        List<SearchResultRow> searchResultRowList = new ArrayList<>();
        SearchResultRow searchResultRow = new SearchResultRow();
        searchResultRow.setItemId(1);
        searchResultRow.setBarcode("123");
        searchResultRow.setTitle("test");
        searchResultRow.setSearchItemResultRows(searchItemResultRowList);
        searchResultRowList.add(searchResultRow);
        return searchResultRowList;

    }



}