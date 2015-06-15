AJS.$(function() {
    var $bitbucketAccessOption = AJS.$('#should-invite-user');
    var $bitbucketInfoIcon = AJS.$('.bitbucket-access-field-group .checkbox .aui-iconfont-info');
    var $jiraApplications = AJS.$('.application-picker-applications input.checkbox.application');
    var $jiraSoftwareAccessOption = AJS.$('.checkbox.application-jira-software');

    function selectBitbucketAccessOnLoadIfSoftwareIsSelected() {
        if($jiraSoftwareAccessOption.is(':checked')) {
            $bitbucketAccessOption.attr('checked', true);
        }
    }

    function atLeastOneJiraApplicationIsSelected() {
        return AJS.$(".application-picker-applications input.checkbox.application:checked").length >= 1;
    }

    function disableBitbuckeAccessOnLoadIfNoJiraApplicationIsSelected() {
        if(!atLeastOneJiraApplicationIsSelected()) {
            disableBitbucketAccessOption();
        }
    }

    function enableBitbucketAccessOption() {
        $bitbucketInfoIcon.addClass('hidden');
        $bitbucketAccessOption.attr('disabled', false);
    }

    function disableBitbucketAccessOption() {
        $bitbucketInfoIcon.removeClass('hidden');
        $bitbucketAccessOption.attr('checked', false).attr('disabled', true);
    }

    function bitbucketAccessOptionIsDisabled() {
        return $bitbucketAccessOption.attr('disabled');
    }

    function prepareBitbucketTeamInlineDialog() {
        AJS.InlineDialog(AJS.$("#add-user-bitbucket-access-extension-panel a.more-teams"), "add-user-bitbucket-access-extension",
                function(content, trigger, showPopup){
                    content.html(dvcs.connector.bitbucket.access.moreTeamsInlineDialogContent({
                        teams: WRM.data.claim('bitbucket-access-inline-dialog-content')
                    }));

                    showPopup();
                    return false;
                }, {
                    width: 250,
                    offsetX: -20
                }
        );
    }

    function prepareInfoIconInlineDialog() {
        AJS.InlineDialog($bitbucketInfoIcon, "bitbucket-access-info-icon",
                function(content, trigger, showPopup){
                    content.html(dvcs.connector.bitbucket.access.infoIconInlineDialogContent({}));
                    showPopup();
                    return false;
                }, {
                    offsetX: -140
                }
        );
    }

    selectBitbucketAccessOnLoadIfSoftwareIsSelected();
    disableBitbuckeAccessOnLoadIfNoJiraApplicationIsSelected();
    prepareBitbucketTeamInlineDialog();
    prepareInfoIconInlineDialog();
    $jiraApplications.on('change', function() {
        var atLeastOneJiraAppIsSelected = atLeastOneJiraApplicationIsSelected();
        var bbOptionDisabled = bitbucketAccessOptionIsDisabled();

        if(atLeastOneJiraAppIsSelected && bbOptionDisabled) {
            enableBitbucketAccessOption();
        }

        if(!atLeastOneJiraAppIsSelected && !bbOptionDisabled) {
            disableBitbucketAccessOption();
        }
    });
});
