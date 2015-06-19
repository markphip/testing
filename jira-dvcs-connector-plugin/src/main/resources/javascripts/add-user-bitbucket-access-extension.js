AJS.$(function() {
    require([
        'dvcs/add-user-form',
        'dvcs/bitbucket-access',
        'dvcs/bitbucket-access-controller',
        'jquery',
        'aui/inline-dialog2'
    ], function(
        AddUserForm,
        BitbucketAccess,
        BitbucketAccessContoller,
        $,
        InlineDialog2 //Unused. To use inline dialog 2, all I need to do is 'require' it.
    ) {
        var $addUserForm = $('#user-create');
        var $bitbucketAccessOption = $('#should-invite-user');
        var $bitbucketInfoIcon = $('.bitbucket-access-field-group .checkbox .aui-iconfont-info');
        var $jiraApplications = $('.application-picker-applications input.checkbox.application');
        var $jiraSoftwareAccessOption = $('.checkbox.application-jira-software');

        var addUserForm = new AddUserForm($jiraApplications, $addUserForm, $jiraSoftwareAccessOption);
        var bitbucketAccess = new BitbucketAccess($bitbucketAccessOption, $bitbucketInfoIcon);
        var bitbucketInviteToGroups = WRM.data.claim('bitbucket-invite-to-groups');
        var bitbucketAccessController = new BitbucketAccessContoller(addUserForm, bitbucketAccess, bitbucketInviteToGroups);
        bitbucketAccessController.start();
    });
});
