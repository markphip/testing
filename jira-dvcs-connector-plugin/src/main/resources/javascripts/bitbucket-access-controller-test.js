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
                    onSubmit: this.sandbox.stub()
                };

                this.bitbucketAccess = {
                    enable: this.sandbox.stub(),
                    disable: this.sandbox.stub(),
                    select: this.sandbox.stub(),
                    deselect: this.sandbox.stub(),
                    isEnabled: this.sandbox.stub(),
                    isSelected: this.sandbox.stub()
                };

                this.bitbucketAccessController = new BitbucketAccessController(this.addUserForm, this.bitbucketAccess);
            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Should establish initial state and register listeners when start is invoked", function(){
            this.sandbox.spy(this.bitbucketAccessController, "establishInitialState");
            this.sandbox.spy(this.bitbucketAccessController, "registerListeners");

            this.bitbucketAccessController.start();

            sinon.assert.calledOnce(this.bitbucketAccessController.establishInitialState);
            sinon.assert.calledOnce(this.bitbucketAccessController.registerListeners);

        });

        test("Should select bitbucket access when Software is selected", function(){
            this.addUserForm.isSoftwareApplicationSelected.returns(true);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.calledOnce(this.bitbucketAccess.select);
        });

        test("Should not select bitbucket access when Software is not selected", function(){
            this.addUserForm.isSoftwareApplicationSelected.returns(false);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.notCalled(this.bitbucketAccess.select);
        });

        test("Should disable bitbucket access when no application is selected", function(){
            this.addUserForm.getSelectedApplicationCount.returns(0);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.calledOnce(this.bitbucketAccess.disable);
        });

        test("Should not disable bitbucket access when some applications are selected", function(){
            this.addUserForm.getSelectedApplicationCount.returns(3);

            this.bitbucketAccessController.establishInitialState();

            sinon.assert.notCalled(this.bitbucketAccess.disable);
        });

        test("Should listen to change and submit events", function(){

            this.bitbucketAccessController.registerListeners();

            sinon.assert.calledOnce(this.addUserForm.onApplicationSelectionChange);
            sinon.assert.calledOnce(this.addUserForm.onSubmit);
        });
        
        test("Should disable bitbucket access when no application is selected and bitbucket access is enabled", function(){
            this.addUserForm.getSelectedApplicationCount.returns(0);
            this.bitbucketAccess.isEnabled.returns(true);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.calledOnce(this.bitbucketAccess.disable);
        });

        test("Should do nothing when no application is selected and bitbucket access is disabled", function(){
            this.addUserForm.getSelectedApplicationCount.returns(0);
            this.bitbucketAccess.isEnabled.returns(false);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.notCalled(this.bitbucketAccess.disable);
        });

        test("Should enable bitbucket access when some applications are selected and bitbucket access is disabled", function(){
            this.addUserForm.getSelectedApplicationCount.returns(3);
            this.bitbucketAccess.isEnabled.returns(false);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.calledOnce(this.bitbucketAccess.enable);
        });

        test("Should do nothing when some applications are selected and bitbucket access is enabled", function(){
            this.addUserForm.getSelectedApplicationCount.returns(3);
            this.bitbucketAccess.isEnabled.returns(true);

            this.bitbucketAccessController.onApplicationSelectionChange();

            sinon.assert.notCalled(this.bitbucketAccess.enable);
        });
    })
});