package info.papyri.dispatch.browse.facet;

import edu.unc.epidoc.transcoder.TransCoder;
import info.papyri.dispatch.FileUtils;
import info.papyri.dispatch.browse.FieldNotFoundException;
import info.papyri.dispatch.browse.SolrField;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

/**
 * Replicates the functioning of <code>info.papyri.dispatch.Search</code> in a 
 * manner compatible with the faceted browse framework and codebase.
 * 
 * Note that although the algorithm used for the search process is intended to
 * replicate that defined in <code>info.papyri.dispatch.Search</code> it is implemented
 * very differently. The logic for string-handling is all encapsulated in the inner 
 * SearchConfiguration class, below and more specific documentation is provided there.
 * 
 * @author thill
 * @version 2011.08.19
 * @see info.papyri.dispatch.Search
 */
public class StringSearchFacet extends Facet{
    
    enum SearchType{ PHRASE, SUBSTRING, LEMMAS, PROXIMITY, WITHIN, USER_DEFINED  };
    enum SearchTarget{ ALL, METADATA, TEXT, TRANSLATIONS, USER_DEFINED };
    enum SearchOption{ BETA, NO_CAPS, NO_MARKS };

    private HashMap<Integer, SearchConfiguration> searchConfigurations = new HashMap<Integer, SearchConfiguration>();
    private static String morphSearch = "morph-search/";

    public StringSearchFacet(){
        
        super(SolrField.transcription_ngram_ia, FacetParam.STRING, "String search");
             
    }
    
    @Override
    public SolrQuery buildQueryContribution(SolrQuery solrQuery){
        
        Iterator<SearchConfiguration> scit = searchConfigurations.values().iterator();
        // TODO: SHIFT ENTIRE RESPONSIBILITY OF THIS INTO SEARCHCONFIGURATION OBJECT
        while(scit.hasNext()){
            
            SearchConfiguration nowConfig = scit.next();
            if(nowConfig.getSearchType().equals(SearchType.USER_DEFINED)){
                
                solrQuery.addFilterQuery(nowConfig.getSearchString());
                continue;
                
            }
            String rawField = "+";
            rawField += nowConfig.getField().name() + ":";
            String searchString = "(" + nowConfig.getSearchString() + ")";
            String fullString = rawField + searchString;
            solrQuery.addFilterQuery(fullString);   
                     
        }
        
        return solrQuery;
        
    }
        
    @Override
    public String generateWidget() {
        
        StringBuilder html = new StringBuilder("<div class=\"facet-widget\" id=\"text-search-widget\" title=\"");
        html.append(getToolTipText());
        html.append("\">");
        html.append(generateHiddenFields());
        
        // textbox HTML
        html.append("<p class=\"ui-corner-all\" id=\"facet-stringsearch-wrapper\">");
        html.append("<input type=\"text\" name=\"");
        html.append(formName.name());
        html.append("\" size=\"50\" maxlength=\"250\" id=\"keyword\"></input>");
        html.append("</p>");
               
        // search type control
        html.append("<div class=\"stringsearch-section\">");
        html.append("<p>");
        html.append("<input class=\"type\" type=\"radio\" name=\"type\" value=\"");
        html.append(SearchType.PHRASE.name().toLowerCase());
        html.append("\" id=\"phrase\"/> ");
        html.append("<label for=\"phrase\" id=\"phrase-label\">Word/Phrase search</label><br/>");
        html.append("<input class=\"type\" type=\"radio\" name=\"type\" value=\"");
        html.append(SearchType.SUBSTRING.name().toLowerCase());
        html.append("\" id=\"substring\" checked/> ");
        html.append("<label for=\"substring\" id=\"substring-label\">Substring search</label><br/>");
        html.append("<input class=\"type\" type=\"radio\" name=\"type\" value=\"");
        html.append(SearchType.LEMMAS.name().toLowerCase());
        html.append("\" id=\"lemmas\"/>");
        html.append("<label for=\"lemmas\" id=\"lemmas-label\">Lemmatized search</label><br/>");
        html.append("<input class=\"type\" type=\"radio\" name=\"type\" value=\"");
        html.append(SearchType.PROXIMITY.name().toLowerCase());
        html.append("\" id=\"proximity\"/>");
        html.append("<label for=\"proximity\" id=\"proximity-label\"> Proximity: find within</label>");
        html.append(" <input type=\"text\" name=\"");
        html.append(SearchType.WITHIN.name().toLowerCase());
        html.append("\" value=\"10\" id=\"within\" size=\"2\" style=\"width:1.5em\"/> words");    
        html.append("</p>");    
        html.append("</div><!-- closing .stringsearch section -->"); 
        
        // search target control
        html.append("<div class=\"stringsearch-section\">");
        html.append("<p>");
        html.append("<input type=\"radio\" name=\"target\" value=\"");
        html.append(SearchTarget.TEXT.name().toLowerCase());
        html.append("\" value=\"on\" id=\"target-text\" class=\"target\" checked/>");
        html.append("<label for=\"");
        html.append(SearchTarget.TEXT.name().toLowerCase());
        html.append("\" id=\"text-label\">Text</label>");
        html.append("<input type=\"radio\" name=\"target\" value=\"");
        html.append(SearchTarget.METADATA.name().toLowerCase());
        html.append("\" value=\"on\" id=\"target-metadata\" class=\"target\"/>");
        html.append("<label for=\"");
        html.append(SearchTarget.METADATA.name().toLowerCase());
        html.append("\" id=\"metadata-label\">Metadata</label>");
        html.append("<input type=\"radio\" name=\"target\" value=\"");        
        html.append(SearchTarget.TRANSLATIONS.name().toLowerCase());
        html.append("\" value=\"on\" id=\"target-translations\" class=\"target\"/>");
        html.append("<label for=\"");
        html.append(SearchTarget.TRANSLATIONS.name().toLowerCase());
        html.append("\" id=\"translation-label\">Translations</label>");
        html.append("<input type=\"radio\" name=\"target\" value=\"");
        html.append(SearchTarget.ALL.name().toLowerCase());
        html.append("\" value=\"on\" id=\"target-all\" class=\"target\"/>");
        html.append("<label for=\"");
        html.append(SearchTarget.ALL.name().toLowerCase());
        html.append("\" id=\"all-label\">All</label>");
        html.append("</p>");
        html.append("</div><!-- closing .stringsearch-section -->");
        
        // search options control
        //html.append("<h3>Options</h3>");
        html.append("<div class=\"stringsearch-section\">");
        html.append("<p><input type=\"checkbox\" name=\"");
        html.append(SearchOption.BETA.name().toLowerCase());
        html.append("\" id=\"betaYes\" value=\"on\"/>");
        html.append("<label for=\"betaYes\" id=\"betaYes-label\">search text in Beta Code</label> <br/>");
        html.append("<input type=\"checkbox\" name=\"");
        html.append(SearchOption.NO_CAPS.name().toLowerCase());
        html.append("\" id=\"caps\" value=\"on\" checked></input>");    // will need to be changed once hooked in
        html.append("<label for=\"caps\" id=\"caps-label\">ignore capitalization</label><br/>");
        html.append("<input type=\"checkbox\" name=\"");
        html.append(SearchOption.NO_MARKS.name().toLowerCase());
        html.append("\" id=\"marks\" value=\"on\" checked></input>");  // will need to be changed once hooked in
        html.append("<label for=\"marks\" id=\"marks-label\">ignore diacritics/accents</label>");
        html.append("</p>");
        html.append("</div><!-- closing .stringsearch-section -->");
        html.append("</div><!-- closing .facet-widget -->");
        return html.toString();
        
    }
    
    @Override
    public Boolean addConstraints(Map<String, String[]> params){
        
        searchConfigurations = pullApartParams(params);
        
        return !searchConfigurations.isEmpty();
        
        
    }
    
    @Override
    String generateHiddenFields(){
        
        StringBuilder html = new StringBuilder();
        
        Iterator<SearchConfiguration> scit = searchConfigurations.values().iterator();
        
        int counter = 1;                
        
        while(scit.hasNext()){
            
            String inp = "<input type=\"hidden\" name=\"";
            String v = "\" value=\"";
            String c = "\"/>";
            SearchConfiguration config = scit.next();
            html.append(inp);
            html.append(formName.name());
            html.append(String.valueOf(counter));
            html.append(v);
            html.append(config.getRawWord().replaceAll("\"", "'"));
            html.append(c);
            
            html.append(inp);
            html.append("target");
            html.append(String.valueOf(counter));
            html.append(v);
            html.append(config.getSearchTarget());
            html.append(c);
            
            html.append(inp);
            html.append("type");
            html.append(String.valueOf(counter));
            html.append(v);
            html.append(config.getSearchType());
            html.append(c);
            
            if(config.getSearchType().equals(SearchType.PROXIMITY)){
                
                html.append(inp);
                html.append(SearchType.WITHIN.name().toLowerCase());
                html.append(String.valueOf(counter));               
                html.append(v);
                html.append(String.valueOf(config.getProximityDistance()));
                html.append(c);
                
            }
            
            if(config.getBetaOn()){
                
                html.append(inp);
                html.append(SearchOption.BETA.name().toLowerCase());
                html.append(String.valueOf(counter));
                html.append(v);
                html.append("on");
                html.append(c);
                
            }
            
            if(config.getIgnoreCaps()){
                
                html.append(inp);
                html.append(SearchOption.NO_CAPS.name().toLowerCase());
                html.append(String.valueOf(counter));                
                html.append(v);
                html.append("on");
                html.append(c);
                
            }
            
            if(config.getIgnoreMarks()){
                
                html.append(inp);
                html.append(SearchOption.NO_MARKS.name().toLowerCase());
                html.append(String.valueOf(counter));
                html.append(v);
                html.append("on");
                html.append(c);
                
            }
            
            counter++;
            
        }
        
        return html.toString();
        
    }
    
    HashMap<Integer, SearchConfiguration> pullApartParams(Map<String, String[]> params){
        
        HashMap<Integer, SearchConfiguration> configs = new HashMap<Integer, SearchConfiguration>();
        
        Pattern pattern = Pattern.compile(formName.name() + "([\\d]*)");
        
        Iterator<String> kit = params.keySet().iterator();
        
        while(kit.hasNext()){
            
            String key = kit.next();
            
            Matcher matcher = pattern.matcher(key);
            
            if(matcher.matches()){
            
                String matchSuffix = matcher.group(1);                        

                String keywordGetter = formName.name() + matchSuffix;
                String typeGetter = "type" + matchSuffix;
                String withinGetter = SearchType.WITHIN.name().toLowerCase() + matchSuffix;
                String targetGetter = "target" + matchSuffix;
                String betaGetter = SearchOption.BETA.name().toLowerCase() + matchSuffix;
                String capsGetter = SearchOption.NO_CAPS.name().toLowerCase() + matchSuffix;
                String marksGetter = SearchOption.NO_MARKS.name().toLowerCase() + matchSuffix;

                if(!params.containsKey(typeGetter) || !params.containsKey(targetGetter)) continue;

                String keyword = params.get(keywordGetter)[0];
                if(keyword == null || "".equals(keyword)) continue;
                String rawSearchType = params.get(typeGetter)[0].toUpperCase();
                String rawSearchTarget = params.get(targetGetter)[0].toUpperCase();
                
                SearchType ty = null;
                SearchTarget trgt = null;
                
                try{
                    
                    ty = SearchType.valueOf(rawSearchType);
                    trgt = SearchTarget.valueOf(rawSearchTarget);
                    
                } catch(IllegalArgumentException iae){
                    
                    continue;
                    
                }
                
                String[] rawBeta = params.get(betaGetter);
                String[] rawCaps = params.get(capsGetter);
                String[] rawMarks = params.get(marksGetter);
                String[] rawDistance = params.get(withinGetter);

                int distance = rawDistance == null ? 0 : rawDistance[0].matches("\\d+") ? Integer.valueOf(rawDistance[0]) : 0;    

                Boolean beta = rawBeta == null ? false : "on".equals(rawBeta[0]);
                Boolean caps = rawCaps == null ? false : "on".equals(rawCaps[0]);
                Boolean marks = rawMarks == null ? false : "on".equals(rawMarks[0]);

                
                SearchConfiguration searchConfig = new SearchConfiguration(keyword, matchSuffix.equals("") ? 0 : Integer.valueOf(matchSuffix), trgt, ty, beta, caps, marks, distance);
                Integer matchNumber = matchSuffix.equals("") ? 0 : Integer.valueOf(matchSuffix);
                configs.put(matchNumber, searchConfig);
                
            }
            
        }
        
        
        return configs;
        
    }
    
    @Override
    public ArrayList<String> getFacetConstraints(String facetParam){
        
        String paramNumber = "0";
        Pattern pattern = Pattern.compile("^.+?(\\d+)$");
        Matcher matcher = pattern.matcher(facetParam);
        if(matcher.matches()){

            paramNumber = matcher.group(1);
            
        }

        ArrayList<String> constraints = new ArrayList<String>();
        constraints.add(paramNumber);
        return constraints;
        
    }
    
    @Override
    public String getDisplayValue(String facetValue){
        
        Integer k = Integer.valueOf(facetValue);
        if(!searchConfigurations.containsKey(k)) return "Facet value not found";
        
        SearchConfiguration config = searchConfigurations.get(k);
        
        StringBuilder dv = new StringBuilder();
        dv.append(config.getRawWord().replaceAll("\\^", "#").replaceAll("'", "\""));
        dv.append("<br/>");
        dv.append("Target: ");
        dv.append(config.getSearchTarget().name().toLowerCase().replace("_", "-"));
        dv.append("<br/>");
        
        if(config.getBetaOn()) dv.append("Beta: On<br/>");
        if(config.getIgnoreCaps()) dv.append("No Caps: On<br/>");
        if(config.getIgnoreMarks()) dv.append("No Marks: On<br/>");
        if(config.getSearchType().equals(SearchType.PROXIMITY)){
            
            dv.append("Within: ");
            dv.append(String.valueOf(config.getProximityDistance()));
            
        }
        
        return dv.toString();
        
    }
    
    @Override
    public String getDisplayName(String param, java.lang.String facetValue){
        
        String paramNumber = "0";
        Pattern pattern = Pattern.compile(this.formName.toString() + "(\\d+)$");
        Matcher matcher = pattern.matcher(param);
        if(matcher.matches()){
            
            paramNumber = matcher.group(1);
            
        }
        
        SearchConfiguration config = searchConfigurations.get(Integer.valueOf(paramNumber));
        
        String searchType = config.getSearchType().name().toLowerCase().replaceAll("_", "-");
           
        String firstCap = searchType.substring(0, 1).toUpperCase();
        return firstCap + searchType.substring(1, searchType.length());
        
        
    }
    
    @Override
    public String getAsQueryString(){
        
        if(searchConfigurations.size() < 1) return "";
        
        StringBuilder qs = new StringBuilder();
        
        for(Map.Entry<Integer, SearchConfiguration> entry : searchConfigurations.entrySet()){
            
            Integer paramNumber = entry.getKey();
            SearchConfiguration config = entry.getValue();
         
            qs.append(getConfigurationAsQueryString(paramNumber, config));
            qs.append("&");
            
        }
        
        String queryString = qs.toString();
        queryString = queryString.substring(0, queryString.length() - 1);
        queryString = queryString.replaceAll("\"", "'");
        return queryString;
    }
    
    @Override
    public String getAsFilteredQueryString(String filterParam, String filterValue){
        
        // filterValue is index to search configuration
        
        StringBuilder qs = new StringBuilder();
        
        for(Map.Entry<Integer, SearchConfiguration> entry : searchConfigurations.entrySet()){
            
            Integer paramNumber = entry.getKey();
            
            if(!String.valueOf(paramNumber).equals(filterValue)){
            
                SearchConfiguration config = entry.getValue();
                qs.append(getConfigurationAsQueryString(paramNumber, config));
                qs.append("&");
                
            }
            
        }
        
        String queryString = qs.toString();
        queryString = queryString.replaceAll("\"", "'");
        if(queryString.endsWith("&")) queryString = queryString.substring(0, queryString.length() - 1);
        return queryString;
        
    }
    
    
    private String getConfigurationAsQueryString(Integer pn, SearchConfiguration config){
        
            String paramNumber = pn == 0 ? "" : String.valueOf(pn);
        
            StringBuilder qs = new StringBuilder();
            String kwParam = formName.name() + paramNumber;
            String typeParam = "type" + paramNumber;
            String targetParam = "target" + paramNumber;
            
            qs.append(kwParam);
            qs.append("=");
            qs.append(config.getRawWord());
            qs.append("&");
            qs.append(typeParam);
            qs.append("=");
            qs.append(config.getSearchType().name());
            qs.append("&");
            qs.append(targetParam);
            qs.append("=");
            qs.append(config.getSearchTarget().name());
            
            if(config.getBetaOn()){
                
                qs.append("&");
                qs.append(SearchOption.BETA.name().toLowerCase());
                qs.append(paramNumber);
                qs.append("=on");
                
            }
            if(config.getIgnoreCaps()){
                
                qs.append("&");
                qs.append(SearchOption.NO_CAPS.name().toLowerCase());
                qs.append(paramNumber);
                qs.append("=on");
                
            }
            if(config.getIgnoreMarks()){
                
                qs.append("&");
                qs.append(SearchOption.NO_MARKS.name().toLowerCase());
                qs.append(paramNumber);
                qs.append("=on");
                
            }
            if(config.getSearchType().equals(SearchType.PROXIMITY)){
                
                qs.append("&");
                qs.append(SearchType.PROXIMITY.name().toLowerCase());
                qs.append(paramNumber);
                qs.append("=on");            
                qs.append("&");
                qs.append(SearchType.WITHIN.name().toLowerCase());
                qs.append(paramNumber);
                qs.append("=");
                qs.append(String.valueOf(config.getProximityDistance()));
            } 
        
        return qs.toString();
        
    }
    
    @Override
    public String getCSSSelectorID(){
        
        return super.getCSSSelectorID() + String.valueOf(searchConfigurations.size());
        
        
    }
    
    @Override
    public void addConstraint(String newValue){
        
        if(newValue.equals(Facet.defaultValue) || "".equals(newValue)) return;
        super.addConstraint(newValue);
        
    }
    
    @Override
    public String[] getFormNames(){

        String[] formNames = new String[searchConfigurations.size()];
        
        Iterator<Integer> pnit = searchConfigurations.keySet().iterator();
        int counter = 0;
        
        while(pnit.hasNext()){
            
            Integer paramNumber = pnit.next();
            
            String paramSuffix = paramNumber.equals(0) ? "" : String.valueOf(paramNumber);
            String param = formName.name().toString() + paramSuffix;
            formNames[counter] = param;
            counter++;
            
        }

        return formNames;
        
    }
         
    @Override
    public void setWidgetValues(QueryResponse queryResponse){}       
         
    
    @Override
    public String getToolTipText(){
        
        return "Performs a substring search, as though using the standard Search page. Capitalisation and diacritcs are ignored.";
        
    }
    
    public String getHighlightString(){
        
        String words = "";
        String nowField = "";
        // TODO: SHIFT RESPONSIBILITY ONTO GETHIGHLIGHTSTRING METHOD OF SEARCHCONFIGURATION OBJECT
        // DESIRED OUTPUT OF THIS METHOD IS SIMPLY CONCATENATED FIELD(VALUE)
        for(SearchConfiguration searchConfiguration : searchConfigurations.values()){
            
            if("".equals(nowField)){
                
                SolrField testField = searchConfiguration.getField();
                if(testField != null){
                    
                    nowField = testField.name();
                    
                }
                
                
            }
            String thisWord = searchConfiguration.getHighlightWord();
 
           /* if(nowField.equals(SolrField.transcription_ia.name())){
                
                try{
                    
                    if(searchConfiguration.getSearchType().equals(SearchType.USER_DEFINED)){
                    
                         thisWord = searchConfiguration.expandLemmas(searchConfiguration.extractLemmaWord(thisWord));
                         
                    }
                    else if(searchConfiguration.getSearchType().equals(SearchType.LEMMAS)) {
                        
                        thisWord = searchConfiguration.expandLemmas(thisWord);
                        
                    }
                    
                } 
                catch (MalformedURLException mue){} 
                catch (SolrServerException sse){}
                
            }*/
            words += " " + thisWord;
                       
        }
        if("".equals(nowField)) nowField = SolrField.transcription_ngram_ia.name();
        if(words.length() > 0) words = words.substring(1);
        String query = nowField + ":(" + words + ")";
        return query;
        
    }
    
    /**
     * This inner class handles the logic for string-searching previously found
     * in <code>info.papyri.dispatch.Search</code>.
     * 
     * Note that while the logic is intended to be the same, the implementation
     * is very different. In particular, the search query is understood to be made
     * up of three parts, which are dealt with separately, as far as this is possible.
     * (1) The search type (substring, phrase, lemmatised, or proximity)
     * (2) The search target (text, metadata, translations, or all three)
     * (3) Transformations to be made to the string itself (e.g., because it is in 
     * betacode format, caps should be ignored, etc.)
     * 
     * In practice these three are inter-related in a cascading fashion - the search type
     * affects the possible search targets and relevant transformations, while
     * the search target affects the possible transformations.
     * 
     * 
     */
    
    class SearchConfiguration{
        
        /** The search string as submitted by the user */
        private String rawString;
        /** The search string: i.e., the rawWord, after it has been
         * subjected to the relevant transformations
         */
        private String searchString;
        /**
         * The search target (text, metadata, translation, or all three) 
         */
        private SearchTarget target;
        /**
         * The search type (phrase, substring, lemmatized, proximity)
         */
        private SearchType type;
        /**
         * The search window used for proximity searches; defaults to 0 for
         * non-proximity searches
         */
        private int proximityDistance;
        /**
         * <code>True</code> if betacode is being used; <code>False</code> otherwise.
         * 
         */
        private Boolean betaOn;
        /** <code>True</code> if capitalisation is to be ignored; <code>False</code>
         *  otherwise.
         */
        private Boolean ignoreCaps;
        /**
         * <code>True</code> if diacritics are to be ignored; <code>False</code>
         * otherwise.
         */
        private Boolean ignoreMarks;
        /**
         * The SolrField that should be used in the search
         * 
         */
        private SolrField field;
        private String highlightWord;
        
        private String[] SEARCH_OPERATORS = {"AND", "OR", "NOT", "&&", "||", "+", "-", "WITHIN", "BEFORE", "AFTER", "IMMEDIATELY-BEFORE", "IMMEDIATELY-AFTER"};
        private HashMap<String, String> STRINGOPS_TO_SOLROPS = new HashMap<String, String>();
        
        SearchConfiguration(String kw, Integer no, SearchTarget tgt, SearchType ty, Boolean beta, Boolean caps, Boolean marks, int wi){
            
            target = tgt;
            type = ty;
            betaOn = beta;
            ignoreCaps = caps;
            ignoreMarks = marks;
            proximityDistance = wi;
            try{
                
                field = setField(kw, type, target, caps, marks);
                
            }
            catch(FieldNotFoundException fnfe){
                
                System.out.println("FieldNotFoundException with search for " + kw + ": " + fnfe.getMessage());
                field = SolrField.all;
                
            }
            rawString = kw;
            try{
                
               searchString = transformSearchString(kw, type, beta, caps, marks);
            
            } catch (MalformedURLException mue){
                
                searchString = "ERROR: Malformed URL " + mue.getMessage();
                
            } catch (SolrServerException sse){
            
                searchString = "ERROR: SolrServerException " + sse.getMessage();
            
            }
            catch(Exception e){
                
                // this will probably be thrown by the Transcoder
                searchString = "ERROR: Probable source of error - " + kw + " is not valid betacode " + e.getMessage();
                
            }
            STRINGOPS_TO_SOLROPS.put("NOT", "-");
            STRINGOPS_TO_SOLROPS.put("WITHIN", "~");
        }
        
        /**
         * Determines the field to search for the string in.
         * 
         * Replicates the logic of info.papyri.dispatch.Search#processRequest
         * 
         * @param st The search's <code>SearchType</code>
         * @param t The search's <code>SearchTarget</code>
         * @param noCaps Boolean - whether or not capitalisation is significant
         * @param noMarks Boolean - whether or not diacritics are significant
         * @return the <code>SolrField</code> to be searched
         * @see info.papyri.dispatch.Search#runQuery(java.io.PrintWriter, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse) 
         */
        
        private SolrField setField(String keyword, SearchType st, SearchTarget t, Boolean noCaps, Boolean noMarks) throws FieldNotFoundException{
            
            if(st.equals(SearchType.USER_DEFINED)){
                
                   SolrField nowField = SolrField.transcription_ngram_ia;
                   if(keyword.contains("lem:")){
                       
                       return SolrField.transcription_ia;
                       
                   }
                   if(keyword.contains(":")){
                       
                       String upToField = keyword.substring(0, keyword.indexOf(":"));
                       String[] bits = upToField.split(" ");
                       for(int i = bits.length - 1; i >= 0; i--){
                           
                           String bit = bits[i];
                           if(!bit.equals("")){
                               
                               for(SolrField sf : SolrField.values()){
                                   
                                   if(bit.equals(sf.name())) nowField = SolrField.valueOf(bit);
                                   
                                   
                               }
                               
                           }
                           
                       }
                                              
                   }
                
                   return nowField;
            }
            
            if(st.equals(SearchType.SUBSTRING)) return SolrField.transcription_ngram_ia;
            
            if(st.equals(SearchType.LEMMAS)) return SolrField.transcription_ia;
            
            // henceforth only proximity and phrase searches possible
            
            if(t.equals(SearchTarget.TEXT)){
                
                if(noCaps && noMarks) return SolrField.transcription_ia;
                
                if(noCaps) return SolrField.transcription_ic;
                        
                if(noMarks) return SolrField.transcription_id;
                
                return SolrField.transcription;
                
            }
                        
            if(t.equals(SearchTarget.METADATA)) return SolrField.metadata;
            
            if(t.equals(SearchTarget.TRANSLATIONS)) return SolrField.translation;
            
            if(t.equals(SearchTarget.ALL)) return SolrField.all;
            
            throw new FieldNotFoundException("Unknown", "With search type " + st.name() + ", search target " + t.name() + ", no caps set to " + noCaps.toString() + ", no diacritics set to " + noMarks.toString());
            
        }
        
        /**
         * Transforms the search string into the appropriate form based on 
         * search settings
         * 
         * 
         * @param rawInput The string as submitted by the user
         * @param st The search's <code>SearchType</code>
         * @param beta <code>Boolean</code>: Whether or not the string is beta-encoded
         * @param noCase <code>Boolean</code>: Whether or not to ignore caps
         * @param noMarks <code>Boolean</code>: Whether or not to ignore diacritics
         * @return <code>String</code> The transformed <code>String</code>
         * @throws SolrServerException
         * @throws MalformedURLException
         * @throws Exception 
         */
        
        private String transformSearchString(String rawInput, SearchType st, Boolean beta, Boolean noCase, Boolean noMarks) throws SolrServerException, MalformedURLException, Exception{
            
            if(st.equals(SearchType.USER_DEFINED)) return getDirectEntryString(rawInput);

            String cleanString = rawInput;
            if(beta){
                
                TransCoder tc = new TransCoder("BetaCodeCaps", "UnicodeC");
                cleanString = tc.getString(cleanString);
                cleanString = cleanString.replace("ΑΝΔ", "AND").replace("ΟΡ", "OR").replace("ΝΟΤ", "NOT");

            }
            if(SearchType.LEMMAS.equals(st)){

                cleanString = FacetBrowser.SOLR_UTIL.expandLemmas(cleanString);
                                  
            }
            // no transform needed for nocaps text - performed by queryanalyzer
            if(noMarks){
                
                cleanString = FileUtils.stripDiacriticals(cleanString);
                
              
            }
            cleanString = cleanString.replaceAll("'", "\"");
            this.highlightWord = cleanString;
            cleanString = cleanString.replaceAll("#", "^");
            cleanString = cleanString.replaceAll("\\^", "\\\\^"); 
            cleanString = cleanString.replaceAll("ς", "σ");
            return cleanString;
            
        }
        
        ArrayList<String> harvestKeywords(String rawInput){
            
            if(rawInput == null) return new ArrayList<String>();
            
            String cleanedInput = rawInput;

            cleanedInput = cleanedInput.replaceAll("[()#]", " ");
            
            for(String operator : this.SEARCH_OPERATORS){
                
                try{
                    
                     if("||".equals(operator)) operator = "\\|\\|";
                     cleanedInput = cleanedInput.replaceAll(operator, " ");
                    
                }
                catch(PatternSyntaxException pse){
                    
                    operator = "\\" + operator;
                    cleanedInput = cleanedInput.replaceAll(operator, " ");
                    
                }
                
            }

            cleanedInput = cleanedInput.replaceAll("[^\\s]+?:", " ");
            cleanedInput = cleanedInput.trim();
            ArrayList<String> inputBits = new ArrayList<String>(Arrays.asList(cleanedInput.split("(\\s)+")));
            return inputBits;
            
        }
        
        ArrayList<String> transformKeywords(ArrayList<String> keywords){
            
            ArrayList<String> transformedKeywords = new ArrayList<String>();
            
            if(keywords == null) return transformedKeywords;
            int counter = 0;
            
            for(String keyword : keywords){
            
                if(ignoreCaps){
                    
                    keyword = keyword.toLowerCase();
                    
                }
                if(betaOn){
               
                    try{
                    
                        String betaWord = "";
                        betaWord = convertToUnicode(keyword); 
                        keyword = betaWord;
                    
                    }
                    catch(Exception e){
                    
                        transformedKeywords.add(keyword);
                        continue;
                    
                    }  
                }
                if(lemmatizeWord(keywords, counter)){
                    
                    try{ 
                        
                        String keywordExpanded = this.expandLemmas(keyword);
                        keyword = "(" + keywordExpanded + ")";
                    
                    }
                    catch(Exception e){
                        
                        transformedKeywords.add(keyword);
                        continue;
                        
                    }
                    
                }
                if(ignoreMarks) keyword = FileUtils.stripDiacriticals(keyword);
                keyword = keyword.replaceAll("#", "^");
                keyword = keyword.replaceAll("\\^", "\\\\^"); 
                
                if(type.equals(SearchType.PHRASE)){
                    
                    if(keyword.indexOf("'") == 0 || keyword.indexOf("\"") == 0) keyword = keyword.substring(1);
                    if(keyword.lastIndexOf("'") == (keyword.length() - 1) || keyword.lastIndexOf("\"") == (keyword.length() - 1)) keyword = keyword.substring(0, keyword.length() - 1);
                    keyword = "\"" + keyword + "\"";
                    
                }
                
                
                
                keyword = keyword.replaceAll("ς", "σ");
                
                
                transformedKeywords.add(keyword);
                counter++;
            
                
            }
            
            return transformedKeywords;
            
        }
        
        private String convertToUnicode(String betaString) throws UnsupportedEncodingException, Exception{
            
            TransCoder tc = new TransCoder("BetaCodeCaps", "UnicodeC");
            String unicodeString = tc.getString(betaString);
            unicodeString = unicodeString.replace("ΑΝΔ", "AND").replace("ΟΡ", "OR").replace("ΝΟΤ", "NOT");           
            return unicodeString;
                
        }
        
        String getDirectEntryString(String rawInput) throws MalformedURLException, SolrServerException{
            
            String deString = rawInput;
            deString = deString.replaceAll("#", "^");
            deString = deString.replaceAll("\\^", "\\\\^"); 
            if(!deString.contains("lem:")) return deString;
            String opener = deString.substring(0, deString.indexOf("lem:"));
            String lemmaWord = extractLemmaWord(rawInput);
            String remainder = deString.substring(deString.indexOf(lemmaWord) + lemmaWord.length());
            String expandedSearchTerm = this.expandLemmas(lemmaWord);
            expandedSearchTerm = expandedSearchTerm.replaceAll("ς", "σ");
            String query = opener + SolrField.transcription_ia.name() + ":(" + expandedSearchTerm + ")" + remainder;
            return query;
                    
        }
        
        Boolean lemmatizeWord(ArrayList<String> keywords, int currentIteration){
            
            if(type.equals(SearchType.LEMMAS)) return true;
            
            if(!type.equals(SearchType.USER_DEFINED)) return false;
            
            String keyword = keywords.get(currentIteration);

            int previousOccurrences = 0;
            
            for(int i = 0; i < currentIteration; i++){
                
                if(keywords.get(i).equals(keyword)) previousOccurrences++;
                
            }

            int index = 0;
            String lemSub = rawString.substring(index);
            int counter = 0;
            
            while(lemSub.contains(keyword)){
                
                index = lemSub.indexOf(keyword);
                if(counter == previousOccurrences) break;
                lemSub = lemSub.substring(index + keyword.length());
                counter++;
                
            }
            
            if(index - "lem:".length() < 0) return false;
            String lemcheck = lemSub.substring(index - "lem:".length(), index);
            if(lemcheck.equals("lem:")) return true;
            return false;
            
        }
        
        private String extractLemmaWord(String rawInput){
            
            String lemmaWord = rawString;
            lemmaWord = lemmaWord.replaceAll("#", "^");
            lemmaWord = lemmaWord.replaceAll("\\^", "\\\\^");
            if(!lemmaWord.contains("lem:")) return "";
            String lemmaWordStart = lemmaWord.substring(lemmaWord.indexOf("lem:") + "lem:".length());
            if(!lemmaWordStart.contains(" ")) return lemmaWordStart;
            lemmaWord = lemmaWordStart.substring(0, lemmaWordStart.indexOf(" "));
            return lemmaWord;
            
        }
        
        /**
         * Gets the search string in a form immediately ready for use in a 
         * <code>SolrQuery</code>
         * 
         * @return 
         */
        
        public String getSearchString(){ 
           
            // transformation only required if a proximity search
           if(type.equals(SearchType.PROXIMITY)){

               return "\"" + searchString + "\"~" + String.valueOf(proximityDistance);
           } 
           
           return searchString;
        
        }
        
        public String expandLemmas(String query) throws MalformedURLException, SolrServerException {
            
            SolrServer solr = new CommonsHttpSolrServer("http://localhost:8083/solr/" + morphSearch);
            StringBuilder exp = new StringBuilder();
            SolrQuery sq = new SolrQuery();
            String[] lemmas = query.split("\\s+");
            for (String lemma : lemmas) {
              exp.append(" lemma:");
              exp.append(lemma);
            }
            sq.setQuery(exp.toString());
            sq.setRows(1000);
            QueryResponse rs = solr.query(sq);
            SolrDocumentList forms = rs.getResults();
            Set<String> formSet = new HashSet<String>();
            if (forms.size() > 0) {
              for (int i = 0; i < forms.size(); i++) {
                formSet.add(FileUtils.stripDiacriticals((String)forms.get(i).getFieldValue("form")).replaceAll("[_^]", "").toLowerCase());
              }
             return FileUtils.interpose(formSet, " OR ");
             
            }
            
            return query;
          }
       
        
        
        /* getters and setters */
        
        public SearchTarget getSearchTarget(){ return target; }
        public String getHighlightWord(){ return highlightWord; }
        public SearchType getSearchType(){ return type; }
        public int getProximityDistance(){ return proximityDistance; }
        public String getRawWord(){ return rawString; }
        public Boolean getBetaOn(){ return betaOn; }
        public Boolean getIgnoreCaps(){ return ignoreCaps; }
        public Boolean getIgnoreMarks(){ return ignoreMarks; }
        public SolrField getField(){ return field; }
    }
    
}
