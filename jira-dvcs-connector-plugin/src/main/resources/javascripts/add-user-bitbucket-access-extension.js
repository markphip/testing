AJS.$(function() {
    AJS.InlineDialog(AJS.$("#add-user-bitbucket-access-extension-panel label[for='should-invite-user'] a"), "add-user-bitbucket-access-extension-dialog",
            function(content, trigger, showPopup){
                content.html('<p>Nothing to see here...move along</p>');
                showPopup();
                return false;
            }
    );
});
