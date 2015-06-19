AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-components", function () {
    "use strict";

    require([
        "dvcs/add-user-form",
        "dvcs/bitbucket-access",
        "dvcs/bitbucket-access-controller"
    ], function(
        AddUserForm,
        BitbucketAccess,
        BitbucketAccessController
    ) {
        module('Bitbucket access controller', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.addUserForm = {
                    isSoftwareApplicationSelected: this.sandbox.stub(),
                    getSelectedApplicationCount: this.sandbox.stub(),
                    onApplicationSelectionChange: this.sandbox.stub(),
                    onSubmit: this.sandbox.stub(),
                    append: this.sandbox.stub()
                };

                this.bitbucketAccess = {
                    enable: this.sandbox.stub(),
                    disable: this.sandbox.stub(),
                    select: this.sandbox.stub(),
                    deselect: this.sandbox.stub(),
                    isEnabled: this.sandbox.stub(),
                    isSelected: this.sandbox.stub()
                };

                var bitbucketInviteToGroups = "1:developers;1:admin;2:developers";
                this.bitbucketAccessController = new BitbucketAccessController(this.addUserForm, this.bitbucketAccess, bitbucketInviteToGroups);
            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Should establish initial state and register listeners when start is invoked", function() {
            this.sandbox.spy(this.bitbucketAccessController, "establishInitialState");
            this.sandbox.spy(this.bitbucketAccessController, "registerListeners");

            this.bitbucketAccessController.start();

            sinon.assert.calledOnce(this.bitbucketAccessController.establishInitialState);
            sinon.assert.calledOnce(this.bitbucketAccessController.registerListeners);

        });

        test("Should select bitbucket access when Software is selected", function() {
            this.addUserForm.isSoftwareApplicationSelected.returns(true);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.calledOnce(this.bitbucketAccess.select);
        });

        test("Should not select bitbucket access when Software is not selected", function() {
            this.addUserForm.isSoftwareApplicationSelected.returns(false);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.notCalled(this.bitbucketAccess.select);
        });

        test("Should disable bitbucket access when no application is selected", function() {
            this.addUserForm.getSelectedApplicationCount.returns(0);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.calledOnce(this.bitbucketAccess.disable);
        });

        test("Should not disable bitbucket access when some applications are selected", function() {
            this.addUserForm.getSelectedApplicationCount.returns(3);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.notCalled(this.bitbucketAccess.disable);
        });

        test("Should listen to change and submit events", function() {

            this.bitbucketAccessController.registerListeners();

            sinon.assert.calledOnce(this.addUserForm.onApplicationSelectionChange);
            sinon.assert.calledOnce(this.addUserForm.onSubmit);
        });
        
        test("Should disable bitbucket access on application selection change when no application is selected and bitbucket access is enabled", function() {
            this.addUserForm.getSelectedApplicationCount.returns(0);
            this.bitbucketAccess.isEnabled.returns(true);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.calledOnce(this.bitbucketAccess.disable);
        });

        test("Should do nothing on application selection change when no application is selected and bitbucket access is disabled", function() {
            this.addUserForm.getSelectedApplicationCount.returns(0);
            this.bitbucketAccess.isEnabled.returns(false);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.notCalled(this.bitbucketAccess.disable);
        });

        test("Should enable bitbucket access on application selection change when some applications are selected and bitbucket access is disabled", function() {
            this.addUserForm.getSelectedApplicationCount.returns(3);
            this.bitbucketAccess.isEnabled.returns(false);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.calledOnce(this.bitbucketAccess.enable);
        });

        test("Should do nothing on application selection change when some applications are selected and bitbucket access is enabled", function() {
            this.addUserForm.getSelectedApplicationCount.returns(3);
            this.bitbucketAccess.isEnabled.returns(true);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.notCalled(this.bitbucketAccess.enable);
        });

        test("Should append hidden input field on form submit when Bitbucket access is selected", function() {
            this.bitbucketAccess.isSelected.returns(true);

            this.bitbucketAccessController.onFormSubmit();

            sinon.assert.calledWithExactly(this.addUserForm.append, '<input type="hidden" name="dvcs_org_selector" value="1:developers;1:admin;2:developers" />');
        });

        test("Should do nothing on form submit when Bitbucket access is not selected", function() {
            this.bitbucketAccess.isSelected.returns(false);

            this.bitbucketAccessController.onFormSubmit();

            sinon.assert.notCalled(this.addUserForm.append);
        });
    })
});