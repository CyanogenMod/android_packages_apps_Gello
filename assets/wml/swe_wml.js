/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

var cardElements;
var currentActiveCard = null;
var defaultActiveCard = null;
// Forward navigation >= 0, backward navigation == -1, unknown navigation == -2
var currentNavigationType = -2;
var blankRegEx = /^\s*$/;

var WMLBrowser = {
    name: "WMLBrowserContext",
    variables: new Object(),
    setVar: function (key, value) {
        this.variables[key] = value;
    },
    getVar: function (key) {
        var value = this.variables[key];
        if (value == null)
            value = '';
        return value;
    },
    newContext: function () {
        this.variables = new Object();
    },
}

function isBlank(str) {
    return (!str || blankRegEx.test(str));
}

function isURLHash(str) {
    return (!isBlank(str) && str.length > 1 && str.indexOf("#") == 0);
}

function getIdFromURLHash(hash) {
    return hash.substr(1);
}

window.onhashchange = function()
{
    console.log("window.onhashchange currentNavigationType = " + currentNavigationType);
    var newHash = window.location.hash;
    if (!isURLHash(newHash)) {
        currentActiveCard.style.display = "none";
        defaultActiveCard.style.display = "initial";
        currentActiveCard = defaultActiveCard;
    } else {
        showCardById(newHash.substr(1));
    }
    updateWMLVariables();
    scheduleTimerTaskIfNeeded(currentActiveCard);
    handleOnNavigationIntrinsicEvent();
};

window.onload = function()
{
    // Consider all the load/reload on this deck as forward navigation.
    currentNavigationType = 1;
    var cardHash = window.location.hash;
    console.log("window.onload card = " + cardHash);
    cardElements = document.getElementsByClassName('wml_card');
    defaultActiveCard = cardElements[0];
    // All the cards are hidden by default.
    // Show the active card onload.
    if (isURLHash(cardHash)) {
        var cardId = cardHash.substr(1);
        for(var i=0, l=cardElements.length; i<l; i++) {
            var card = cardElements[i];
            if (card.getAttribute("id") == cardId) {
                currentActiveCard = card;
                currentActiveCard.style.display = "initial";
                break;
            }
        }
    }
    if (!currentActiveCard) {
        currentActiveCard = defaultActiveCard;
        currentActiveCard.style.display = "initial";
    }
    replaceVariablesInTextContentBySpan();
    fixTextContentInAnchorTasks();
    initializeSelectElements();
    parseFormatAttributeInInputtElements();
    scheduleTimerTaskIfNeeded(currentActiveCard);
    handleOnNavigationIntrinsicEvent();
};

function showCardById(cardId, onload)
{
    if (currentActiveCard && currentActiveCard.getAttribute("id") == cardId) {
        // We have nothing to do.
        return false;
    }
    for(var i=0, l=cardElements.length; i<l; i++) {
        var card = cardElements[i];
        if (card.getAttribute("id") == cardId) {
            currentActiveCard.style.display = "none";
            currentActiveCard = card;
            currentActiveCard.style.display = "initial";
            return true;
        }
    }
    return false;
}

function handleOnNavigationIntrinsicEvent() {
    var navigationType = currentNavigationType;
    currentNavigationType = -2;

    if (navigationType >= 0) {
        executeOnenterforwardTask();
    } else if (navigationType == -1) {
        executeOnenterbackwardTask();
    } else {
        console.log("WARNING: Cannot determine the navigation event type on this card!");
    }
}

////////////////////////// WML Variables ////////////////////////////////////////
function replaceVariablesInTextContentBySpan()
{
    var pattern1 = /(\$\(([_a-z]{1}[_a-z0-9]*)([:]{1}((([e]{1})(scape)?)|(([n]{1})(oesc)?)|(([u]{1})(nesc)?)))?\))/gi;
    var pattern2 = /(\$([_a-zA-z]{1}[_a-zA-Z0-9]*))/g;
    var whitespace = /^\s*$/g;
    var replacer = function () {
        var name = arguments[2];
        var escape = "";
        if (arguments[12])
            escape = arguments[12];
        else if (arguments[9])
            escape = arguments[9];
        else if (arguments[6])
            escape = arguments[6];
        var wml_variable_span = "\<span\ data\-wml\_name\=\"" + name + "\"\ class\=\"wml_variable\" data\-wml\_escape\=\"" + escape + "\"\>\<\/span\>";
        console.log("replaceVariablesInTextContentBySpan() Found variable " + arguments[0]);
        return wml_variable_span;
    };

    var textNodes = [];
    var treeWalker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
    while(treeWalker.nextNode()) {
        textNodes.push(treeWalker.currentNode);
    }

    for(var i=0, l=textNodes.length; i<l; i++) {
        var curNode = textNodes[i];
        var text = curNode.nodeValue;
        if (!(whitespace.test(text))) {
            var replaced = false;
            if (pattern1.test(text)) {
                text = text.replace(pattern1, replacer);
                replaced = true;
            }
            if (pattern2.test(text)) {
                text = text.replace(pattern2, replacer);
                replaced = true;
            }
            if (replaced) {
                var mySpan = document.createElement("span");
                mySpan.innerHTML = text;
                curNode.parentNode.replaceChild(mySpan, curNode);
            }
        }
    }
}

////////////////////////// WML Anchor Tasks ////////////////////////////////////////
// TODO: Optimize this brutal code
function fixTextContentInAnchorTasks()
{
    var anchorTasks = document.getElementsByClassName('wml_anchor_task');
    for(var i=0, l=anchorTasks.length; i<l; i++) {
        var container;
        var taskParent;
        var task = anchorTasks[i];
        if (task.dataset.wml_task_type == 'wml_task_go') {
            taskParent = task.parentNode;
            container = taskParent.parentNode;
        } else {
            taskParent = task;
            container = task.parentNode;
        }
        container.removeChild(taskParent);
        var anchorText = container.innerHTML;
        while (container.firstChild) {
            container.removeChild(container.firstChild);
        }
        container.appendChild(taskParent);
        console.log("fixTextContentInAnchorTasks: anchorText = " + anchorText);
        task.innerHTML = anchorText + task.innerHTML;
    }
}

////////////////////////// WML Timer ////////////////////////////////////////
var activeScheduledTimer = null;
function scheduleTimerTaskIfNeeded(card)
{
    var timerElements = card.getElementsByClassName('wml_timer');
    if (timerElements.length > 0) {
        var timeout = timerElements[0].dataset.wml_value;
        activeScheduledTimer = window.setTimeout(executeTimerTask, timeout * 100);
        console.log("Starting WML timer timeout = " + timeout);
    }
}

function cancelScheduledTimerTask()
{
    if (activeScheduledTimer != null) {
        window.clearTimeout(clearTimeout);
        activeScheduledTimer = null;
    }
}

////////////////////////////////// <select> & <option> ////////////////////////
var multiSelectSeparator = ";";
var selectElementsMap = new Object();
var WMLSelectElements = {
    name: "WMLSelectElementsList",
    options: new Object(),
    setSelectedOptions: function (key, options) {
        this.options[key] = options;
    },
    getSelectedOptions: function (key) {
        return this.options[key];
    },
};

function initializeSelectElements()
{
    var selectElements = document.getElementsByClassName('wml_select');
    for(var i=0, l=selectElements.length; i<l; i++) {
        var select = selectElements[i];
        var iname = select.dataset.wml_iname;
        // Preselect options based on 'ivalue'
        var ivalue = select.dataset.wml_ivalue;
        if (!isBlank(ivalue)) {
            var options = select.options;
            var optionsCount = options.length;
            var ivalueList = ivalue.split(multiSelectSeparator);
            for(var j=0, ll=ivalueList.length; j<ll; j++) {
                var index = parseInt(ivalueList[j], 10);
                if (index > 0 && index <= optionsCount) {
                    options[index-1].selected = 'selected';
                }
            }
        }

        // Keep a copy of selected options to process 'onpick' events
        var options = select.selectedOptions;
        var optionsCount = options.length;
        var optionsList = [];
        for(var j=0; j<optionsCount; j++) {
            optionsList.push(options[j]);
        }
        var uniqueId = "wml_select_" + i;
        select.dataset.wml_unique_id = uniqueId;
        WMLSelectElements.setSelectedOptions(uniqueId, optionsList);
    }
}

function handleSelectOnchangeEvent(select)
{
    refreshVariableInSelectElement(select);
    var options = select.selectedOptions;
    var optionsCount = options.length;
    var uniqueId = select.dataset.wml_unique_id;
    var oldSelectedOptions = WMLSelectElements.getSelectedOptions(uniqueId);

    // Update the copy of selected options before we do anything
    var optionsList = [];
    for(var j=0; j<optionsCount; j++) {
        optionsList.push(options[j]);
    }
    WMLSelectElements.setSelectedOptions(uniqueId, optionsList);

    // process 'onpick' events if needed
    for(var i=0; i<optionsCount; i++) {
        var option = options[i];
        var onpick = option.dataset.wml_onpick;
        if (!isBlank(onpick)) {
            var selectedNewly = true;
            for(var j=0; j<oldSelectedOptions.length; j++) {
                if (option.isSameNode(oldSelectedOptions[j])) {
                    selectedNewly = false;
                    break;
                }
            }
            if (selectedNewly) {
                internal_executeOnpickTask(option);
                return;
            }
        }
    }
}

function handleAOnClick(event, node)
{
    var href = node.href;
    var search = node.search;
    console.log("handleAOnClick <a> href = " + href);
    refreshVariablesInControlElements();
    if (!isBlank(search)) {
        node.search = substituteVariablesInURL(search);
    } else {
        node.href = href.split("?")[0];
    }
    event.preventDefault();
    navigateToURL(node.href);
    return false;
}

//////////////////////// Variables /////////////////////////
function substituteVariablesInURL(url)
{
    var pattern = /(((\%24)|(\$))\(([_a-z]{1}[_a-z0-9]*)([:]{1}((([e]{1})(scape)?)|(([n]{1})(oesc)?)|(([u]{1})(nesc)?)))?\))/gi;
    var replacer = function () {
        var name = arguments[2];
        // TODO: Do the URL escaping here
        console.log("substituteVariablesInURL() found variable : " + arguments[0]);
        return WMLBrowser.getVar(name);
    };
    return url.replace(pattern, replacer);
}

function substituteVariablesInPostfield(value)
{
    var pattern1 = /(\$\(([_a-z]{1}[_a-z0-9]*)([:]{1}((([e]{1})(scape)?)|(([n]{1})(oesc)?)|(([u]{1})(nesc)?)))?\))/gi;
    var pattern2 = /(\$([_a-zA-z]{1}[_a-zA-Z0-9]*))/g;
    var replacer = function () {
        var name = arguments[2];
        // TODO: Do the URL escaping here
        console.log("substituteVariablesInPostfield() found variable : " + arguments[0]);
        return WMLBrowser.getVar(name);
    };
    if (pattern1.test(value)) {
        return value.replace(pattern1, replacer);
    } else if (pattern2.test(value)) {
        return value.replace(pattern2, replacer);
    }
}

function refreshVariableInSelectElement(select)
{
    var options = select.selectedOptions;
    var value = "";
    var ivalue = "";
    var optionsCount = options.length;
    if (optionsCount > 0) {
        var op = options[0];
        value = op.value;
        ivalue = (op.index + 1);
        for(var i=1; i<optionsCount; i++) {
            op = options[i];
            value += multiSelectSeparator + op.value;
            ivalue += multiSelectSeparator + (op.index + 1);
        }
    }
    var name = select.name;
    if (!isBlank(name)) {
        WMLBrowser.setVar(name, value);
        console.log("refreshVariableInSelectElement name = " + name + ", value = " + value);
    }
    var iname = select.dataset.wml_iname;
    if (!isBlank(iname)) {
        if (isBlank(ivalue)) {
            // An index of zero indicates that no option is selected.
            ivalue = "0";
        }
        WMLBrowser.setVar(iname, ivalue);
        console.log("refreshVariableInSelectElement iname = " + iname + ", ivalue = " + ivalue);
    }
}

function refreshVariablesInControlElements()
{
    var inputElements = currentActiveCard.getElementsByClassName('wml_input');
    for(var i=0, l=inputElements.length; i<l; i++) {
        var input = inputElements[i];
        WMLBrowser.setVar(input.name, input.value);
        console.log("refreshVariablesInControlElements <input> name = " + input.name + ", value = " + input.value);
    }

    var selectElements = currentActiveCard.getElementsByClassName('wml_select');
    for(var i=0, l=selectElements.length; i<l; i++) {
        var select = selectElements[i];
        refreshVariableInSelectElement(selectElements[i]);
    }
}

var validateInputValue = function(event) {
    var input = event.target;
    var pattern = new RegExp(input.dataset.wml_pattern, "g");
    var value = input.value;
    if (!pattern.test(value)) {
        input.value = input.dataset.old_value;
    } else {
        input.dataset.old_value = input.value;
    }
}

function parseFormatAttributeInInputtElements() {

    var regex = /(^(([\*]{1})|([0-9]*))([NnXxAa]{1})$)/g;
    var symbols = "\!\"\#\$\%\&\'\(\)\*\+\,\\.\/\:\;\<\=\>\?\@\\\^\_\`\{\|\}\~\\-\\\\[\\]";

    var inputElements = document.getElementsByClassName('wml_input');
    for(var i=0, l=inputElements.length; i<l; i++) {
        var input = inputElements[i];
        var format = input.dataset.wml_format;
        var res = regex.test(format);
        regex.lastIndex = 0;

        if (res) {
            var pattern = "";
            var fl = format.length;
            var count = format.slice(0, fl-1);
            switch(format.charAt(fl-1)) {
                case 'A':
                    // WML-1.3: Entry of any uppercase letter, symbol, or punctuation character. Numeric characters are excluded.
                    // RegEx: Everthing else except lowercase letters and nummeric characters.
                    pattern = "[^a-z^0-9]";
                    break;
                case 'a':
                    // WML-1.3: Entry of any lowercase letter, symbol, or punctuation character. Numeric characters are excluded.
                    // RegEx: Everthing else except uppercase letters and nummeric characters.
                    pattern = "[^A-Z^0-9]";
                    break;
                case 'N':
                    // WML-1.3: entry of any numeric character.
                    pattern = "[0-9]";
                    input.inputmode = "numeric";
                    break;
                case 'n':
                    // WML-1.3: entry of any numeric, symbol, or punctuation character.
                    pattern = "[0-9" + symbols + "]";
                    break;
                case 'X':
                    // WML-1.3: entry of any uppercase letter, numeric character, symbol, or punctuation character.
                    pattern = "[^a-z]";
                    input.inputmode = "verbatim";
                    break;
                case 'x':
                    // WML-1.3: entry of any lowercase letter, numeric character, symbol, or punctuation character.
                    pattern = "[^A-Z]";
                    input.inputmode = "verbatim";
                    break;
            }

            if (count == "*") {
                pattern = "^(" + pattern + "*)$";
            } else {
                input.maxLength = count;
                input.pattern = ".{" + count + "}";
                pattern = "^(" + pattern + "{0," + count + "})$";
            }
            input.dataset.wml_pattern = pattern;
            input.setAttribute("required", "");
            input.addEventListener("input", validateInputValue);
            console.log("WML input format = " + format + ", equivalent pattern = " + pattern);
        }
    }
}

function updateVariableInPostfields(form)
{
    var postfields = currentActiveCard.getElementsByClassName('wml_postfield');
    for(var i=0, l=postfields.length; i<l; i++) {
        var input = postfields[i];
        input.value = substituteVariablesInPostfield(input.value);
        console.log("updateVariableInPostfields <postfield> name = " + input.name + ", value = " + input.value);
    }
}

/////////////// Navigation ////////////////////
function navigateToURL(url)
{
    console.log("navigateToURL: url = " + url);
    cancelScheduledTimerTask();
    currentNavigationType = 1;
    window.location = url;
}

function navigateToCard(card)
{
    console.log("navigateToCard: card = " + card);
    cancelScheduledTimerTask();
    currentNavigationType = 1;
    window.location.hash = card;
}

function navigateBack()
{
    console.log("navigateBack: currentState = ");
    console.log(window.history.state);
    cancelScheduledTimerTask();
    currentNavigationType = -1;
    window.history.back();
}

////////////// WML Tasks //////////////////////
//<refresh>

function updateWMLVariables()
{
    var wmlVariables = currentActiveCard.getElementsByClassName('wml_variable');
    for(var i=0, l=wmlVariables.length; i<l; i++) {
        var varElement = wmlVariables[i];
        // Handle the variable escaping option here
        var value = WMLBrowser.getVar(varElement.dataset.wml_name);
        // TODO: Handle nested variable substitution on 'value'
        var conversion = varElement.dataset.wml_escape;
        if (!isBlank(value) && !isBlank(conversion)) {
            if (conversion == "e" || conversion == "E") {
                value = encodeURIComponent(value);
            } else if (conversion == "u" || conversion == "U") {
                value = decodeURIComponent(value);
            }
        }
        varElement.innerHTML = value;
    }
}

function internal_executeRefreshTask(root)
{
    console.log("internal_executeRefreshTask");
    var setvarElements = root.getElementsByClassName('wml_setvar');
    for(var i=0, l=setvarElements.length; i<l; i++) {
        var setvar = setvarElements[i];
        WMLBrowser.setVar(setvar.dataset.wml_name, setvar.dataset.wml_value);
        console.log("<setvar> " + setvar.dataset.wml_name + " = " + setvar.dataset.wml_value);
    }
    updateWMLVariables();
}

function executeRefreshTask(event, node)
{
    event.preventDefault();
    internal_executeRefreshTask(node.parentNode);
    return false;
}

// <prev>
function internal_executePrevTask(node)
{
    console.log("internal_executePrevTask");
    internal_executeRefreshTask(node.parentNode);
    navigateBack();
}

function executePrevTask(event, node)
{
    event.preventDefault();
    internal_executePrevTask(node);
    return false;
}
// <option onpick="...">
function internal_executeOnpickTask(option)
{
    var href = option.dataset.wml_onpick;
    console.log("internal_executeOnpickTask href = " + href);
    //internal_executeRefreshTask(option.parentNode);
    //refreshVariablesInControlElements();
    if (isURLHash(href)) {
        navigateToCard(href);
        return true;
    }
    navigateToURL(href);
    return true;
}

//<go>
function addQueryStringKeyValuePairsToForm(form)
{
    var href = form.dataset.wml_href;
    var query;

    // Seperate the query string from the href
    var queryFragments = href.split("?");
    if (queryFragments.length === 2) {
        query = queryFragments[1];
    } else {
        queryFragments.shift();
        query = queryFragments.join("?");
    }

    // Parse the query string for key/value pairs.  Add them to the form
    // as hidden input elements.
    // E.g., http://myserver/test.wml?p1=foo&p2=bar
    // would add the following to the form:
    //   <input type="hidden" name="p1" value="foo">
    //   <input type="hidden" name="p2" value="bar">
    query.replace(
        new RegExp( "([^?=&]+)(?:=([^&]*))?", "g" ),
            function(match, name, value) {
                var param = document.createElement("input");
                param.setAttribute("type","hidden")
                param.setAttribute("name", name)
                param.setAttribute("value",value)
                form.appendChild(param)
           }
        );
    return true;
}

function internal_executeGoTask(form)
{
    var href = form.dataset.wml_href;
    console.log("internal_executeGoTask href = " + href);
    internal_executeRefreshTask(form.parentNode);
    refreshVariablesInControlElements();
    if (isURLHash(href)) {
        navigateToCard(href);
        return true;
    }
    // Substitute variables in <postfield> 'value' attributes before form submission.
    updateVariableInPostfields();
    addQueryStringKeyValuePairsToForm(form);
    form.submit();
    return false;
}

function executeGoTask(event, form)
{
    event.preventDefault();
    internal_executeGoTask(form);
    return false;
}

//<onevent>
function executeTimerTask()
{
    console.log("executeTimerTask()");
    activeScheduledTimer = null;
    // Handle <card ontimer="..."> event first
    var ontimer = currentActiveCard.dataset.wml_ontimer;
    if (!isBlank(ontimer))
    {
        navigateToURL(ontimer);
        return;
    }
    // Handle <onevent type="timer">... here
    var tasks = currentActiveCard.getElementsByClassName('wml_onevent_ontimer');
    if (tasks.length > 0) {
        var onevent = tasks[0];
        if (onevent.dataset.wml_task_type == 'wml_task_go')
            return internal_executeGoTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_refresh')
            return internal_executeRefreshTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_prev')
            return internal_executePrevTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_noop')
            return;
    }
    // Handle <template timer="..."> event at the end
    var templates = currentActiveCard.getElementsByClassName('wml_template');
    if (templates.length > 0) {
        var ontimer = templates[0].dataset.wml_ontimer;
        if (!isBlank(ontimer))
        {
            navigateToURL(ontimer);
            return;
        }
    }
}

function executeOnenterforwardTask()
{
    console.log("executeOnenterforwardTask()");
    // Handle <card onenterforward="..."> event first
    var onef = currentActiveCard.dataset.wml_onenterforward;
    if (!isBlank(onef))
    {
        navigateToURL(onef);
        return;
    }
    // Handle <onevent type="onenterforward">... here
    var tasks = currentActiveCard.getElementsByClassName('wml_onevent_onenterforward');
    if (tasks.length > 0) {
        var onevent = tasks[0];
        if (onevent.dataset.wml_task_type == 'wml_task_go')
            return internal_executeGoTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_refresh')
            return internal_executeRefreshTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_prev')
            return internal_executePrevTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_noop')
            return;
    }
    // Handle <template onenterforward="..."> event at the end
    var templates = currentActiveCard.getElementsByClassName('wml_template');
    if (templates.length > 0) {
        var onef = templates[0].dataset.wml_onenterforward;
        if (!isBlank(onef))
        {
            navigateToURL(onef);
            return;
        }
    }
}

function executeOnenterbackwardTask()
{
    console.log("executeOnenterbackwardTask()");
    // Handle <card onenterforward="..."> event first
    var oneb = currentActiveCard.dataset.wml_onenterbackward;
    if (!isBlank(oneb))
    {
        navigateToURL(oneb);
        return;
    }
    // Handle <onevent type="onenterbackward">... here
    var tasks = currentActiveCard.getElementsByClassName('wml_onevent_onenterbackward');
    if (tasks.length > 0) {
        var onevent = tasks[0];
        if (onevent.dataset.wml_task_type == 'wml_task_go')
            return internal_executeGoTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_refresh')
            return internal_executeRefreshTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_prev')
            return internal_executePrevTask(onevent);
        else if (onevent.dataset.wml_task_type == 'wml_task_noop')
            return;
    }
    // Handle <template onenterbackward="..."> event at the end
    var templates = currentActiveCard.getElementsByClassName('wml_template');
    if (templates.length > 0) {
        var oneb = templates[0].dataset.wml_onenterbackward;
        if (!isBlank(oneb))
        {
            navigateToURL(oneb);
            return;
        }
    }
}


