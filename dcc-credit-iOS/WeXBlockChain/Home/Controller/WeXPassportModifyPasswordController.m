//
//  WeXPassportModifyPasswordController.m
//  WeXBlockChain
//
//  Created by wcc on 2017/11/21.
//  Copyright © 2017年 WeX. All rights reserved.
//

#import "WeXPassportModifyPasswordController.h"
#import "WeXPassportBackupViewController.h"

#import "WeXPasswordModifySuccessFloatView.h"


@interface WeXPassportModifyPasswordController ()<WeXPasswordManagerDelegate,WeXPasswordModifySuccessFloatViewDelegate>
{
    WeXPasswordCacheModal *_model;
    
    WeXPasswordManager *_manager;
    
    WeXPasswordModifySuccessFloatView *_successView;
}

@property (nonatomic,strong)UITextField *oldPsdTextField;

@property (nonatomic,strong)UITextField *newestPsdTextField;

@end

@implementation WeXPassportModifyPasswordController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationItem.title = @"口袋密码修改";
    [self setNavigationNomalBackButtonType];
    [self commonInit];
    [self setupSubViews];
}

- (void)commonInit{
    _model = [WexCommonFunc getPassport];
    NSLog(@"_model=%@",_model);
    
}

//初始化滚动视图
-(void)setupSubViews{
    //显示密码按钮
    UIButton *showBtn1 = [UIButton buttonWithType:UIButtonTypeCustom];
    [showBtn1 setImage:[UIImage imageNamed:@"eye2"] forState:UIControlStateNormal];
    [showBtn1 setImage:[UIImage imageNamed:@"eye1"] forState:UIControlStateSelected];
    [showBtn1 addTarget:self action:@selector(showBtn1Click:) forControlEvents:UIControlEventTouchUpInside];
    showBtn1.titleLabel.font = [UIFont systemFontOfSize:15];
    [self.view addSubview:showBtn1];
    [showBtn1 mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.view).offset(kNavgationBarHeight+40);
        make.trailing.equalTo(self.view).offset(-20);
        make.width.equalTo(@30);
        make.height.equalTo(@45);
    }];
    //原始密码输入框
    _oldPsdTextField = [[UITextField alloc] init];
    _oldPsdTextField.borderStyle = UITextBorderStyleNone;
    _oldPsdTextField.secureTextEntry = YES;
    UILabel *leftLabel1 = [[UILabel alloc] initWithFrame:CGRectMake(0, 0,170,45)];
    leftLabel1.text = @"请输入原口袋密码:";
    leftLabel1.font = [UIFont systemFontOfSize:17];
    leftLabel1.textColor = ColorWithLabelDescritionBlack;
    _oldPsdTextField.backgroundColor = [UIColor clearColor];
    _oldPsdTextField.leftViewMode = UITextFieldViewModeAlways;
    _oldPsdTextField.leftView = leftLabel1;
    _oldPsdTextField.textColor = ColorWithLabelDescritionBlack;
    _oldPsdTextField.font = [UIFont systemFontOfSize:17];
    [self.view addSubview:_oldPsdTextField];
    [_oldPsdTextField mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(showBtn1).offset(0);
        make.leading.equalTo(self.view).offset(20);
        make.trailing.equalTo(showBtn1.mas_leading);
        make.height.equalTo(@45);
    }];
    
    UIView *line1 = [[UIView alloc] init];
    line1.backgroundColor = ColorWithLine;
    [self.view addSubview:line1];
    [line1 mas_makeConstraints:^(MASConstraintMaker *make) {
        make.leading.equalTo(_oldPsdTextField.mas_leading);
        make.trailing.equalTo(showBtn1);
        make.top.equalTo(_oldPsdTextField.mas_bottom);
        make.height.equalTo(@LINE_VIEW_Width);
    }];
    
    
    //显示密码按钮
    UIButton *showBtn2 = [UIButton buttonWithType:UIButtonTypeCustom];
    [showBtn2 setImage:[UIImage imageNamed:@"eye2"] forState:UIControlStateNormal];
    [showBtn2 setImage:[UIImage imageNamed:@"eye1"] forState:UIControlStateSelected];
    [showBtn2 addTarget:self action:@selector(showBtn2Click:) forControlEvents:UIControlEventTouchUpInside];
    showBtn2.titleLabel.font = [UIFont systemFontOfSize:15];
    [self.view addSubview:showBtn2];
    [showBtn2 mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(line1.mas_bottom).offset(0);
        make.trailing.equalTo(self.view).offset(-20);
        make.width.equalTo(@30);
        make.height.equalTo(@45);
    }];
    
    //新密码输入框
    _newestPsdTextField = [[UITextField alloc] init];
    _newestPsdTextField.borderStyle = UITextBorderStyleNone;
    _newestPsdTextField.secureTextEntry = YES;
    UILabel *leftLabel2 = [[UILabel alloc] initWithFrame:CGRectMake(0,0,170,45)];
    leftLabel2.text = @"请输入新口袋密码:";
    leftLabel2.font = [UIFont systemFontOfSize:17];
    leftLabel2.textColor = ColorWithLabelDescritionBlack;
    leftLabel2.backgroundColor = [UIColor clearColor];
    _newestPsdTextField.leftViewMode = UITextFieldViewModeAlways;
    _newestPsdTextField.leftView = leftLabel2;;
    _newestPsdTextField.textColor = ColorWithLabelDescritionBlack;
    _newestPsdTextField.font = [UIFont systemFontOfSize:17];
    [self.view addSubview:_newestPsdTextField];
    [_newestPsdTextField mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(line1.mas_bottom);
        make.leading.equalTo(line1);
        make.trailing.equalTo(showBtn2.mas_leading);
        make.height.equalTo(@45);
    }];
    
    UIView *line2 = [[UIView alloc] init];
    line2.backgroundColor = ColorWithLine;
//    line2.alpha = LINE_VIEW_ALPHA;
    [self.view addSubview:line2];
    [line2 mas_makeConstraints:^(MASConstraintMaker *make) {
        make.leading.equalTo(self.view).offset(20);
        make.trailing.equalTo(self.view).offset(-20);
        make.top.equalTo(_newestPsdTextField.mas_bottom);
        make.height.equalTo(@LINE_VIEW_Width);
    }];
    
    //修改口袋密码按钮
    UIButton *modifyBtn = [UIButton buttonWithType:UIButtonTypeCustom];
    [modifyBtn setTitle:@"修改口袋密码" forState:UIControlStateNormal];
    [modifyBtn setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    modifyBtn.backgroundColor = ColorWithButtonRed;
    modifyBtn.layer.cornerRadius = 3;
    modifyBtn.layer.masksToBounds = YES;
    [modifyBtn addTarget:self action:@selector(modifyBtnClick) forControlEvents:UIControlEventTouchUpInside];
    modifyBtn.titleLabel.font = [UIFont systemFontOfSize:18];
    [self.view addSubview:modifyBtn];
    [modifyBtn mas_makeConstraints:^(MASConstraintMaker *make) {
        make.bottom.equalTo(self.view).offset(-40);
        make.trailing.equalTo(self.view).offset(-20);
        make.leading.equalTo(self.view).offset(20);
        make.height.equalTo(@40);
    }];
}

- (void)showBtn1Click:(UIButton *)btn{
    btn.selected = !btn.selected;
    _oldPsdTextField.secureTextEntry = !btn.selected;
}

- (void)showBtn2Click:(UIButton *)btn{
    btn.selected = !btn.selected;
    _newestPsdTextField.secureTextEntry = !btn.selected;
}

- (void)modifyBtnClick{
    
     //密码长度错误
    if (self.oldPsdTextField.text.length<2|| self.oldPsdTextField.text.length>20) {
        [WeXPorgressHUD showText:@"密码长度为2-20位!" onView:self.view];
        return;
    }
     //密码长度错误
    if (self.newestPsdTextField.text.length<2|| self.newestPsdTextField.text.length>20) {
        [WeXPorgressHUD showText:@"密码长度为2-20位!" onView:self.view];
        return;
    }
    //密码验证错误
    _model = [WexCommonFunc getPassport];
    if (![self.oldPsdTextField.text isEqualToString:_model.passportPassword]) {
        [WeXPorgressHUD showText:@"您的密码有误!" onView:self.view];
        return;
    }
    
    [self configLocalSafetyView];
}

#pragma mark - TextField代理方法
-(BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [self.view endEditing:YES];
    return YES;
}

-(BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string
{
    
    NSString *comment;
    if(range.length == 0)
    {
        comment = [NSString stringWithFormat:@"%@%@",textField.text, string];
        if (comment.length > 20) {
            [WeXPorgressHUD showText:@"密码长度最多为20位" onView:self.view];
            return NO;
        }
    }
    else
    {
        comment = [textField.text substringToIndex:textField.text.length -range.length];
    }
    NSLog(@"comment=%@",comment);
    return YES;
}

-(void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event
{
    [self.view endEditing:YES];
}

- (void)createSuccessFloatView{
    
    [WeXPorgressHUD showLoadingAddedTo:self.view];
    
    // 初始化以太坊容器
    [[WXPassHelper instance] initPassHelperBlock:^(id response) {
        if(response!=nil)
        {
            NSError* error=response;
            NSLog(@"容器加载失败:%@",error);
            return;
        }
        /** 连接以太坊(开发，测试，生产环境地址值不同，建议用宏区分不同开发环境) */
        [[WXPassHelper instance] initProvider:YTF_DEVELOP_SERVER responseBlock:^(id response) {
            NSLog(@"nitProvide=%@",response);
            [[WXPassHelper instance] keystoreCreateWithPrivateKey:_model.walletPrivateKey password:self.newestPsdTextField.text responseBlock:^(id response) {
                
                [WeXPorgressHUD hideLoading];
                
                _model = [WexCommonFunc getPassport];
                _model.keyStore = response;
                _model.passportPassword = self.newestPsdTextField.text;
                [WexCommonFunc savePassport:_model];
                
                _successView = [[WeXPasswordModifySuccessFloatView alloc] initWithFrame:self.view.bounds];
                _successView.delegate = self;
                [self.view addSubview:_successView];
                
                _oldPsdTextField.text = @"";
                _newestPsdTextField.text = @"";
                
            }];
            
        }];
    }];
    
    
    
  
  
}


- (void)configLocalSafetyView{
    
    WeXPasswordCacheModal *model = [WexCommonFunc getPassport];
    
    if (model.passwordType == WeXPasswordTypeNone)
    {
        [self createSuccessFloatView];
    }
    else
    {
        WeXPasswordManager *manager = [WeXPasswordManager managerWithType:WeXPasswordManagerTypeVerify parentController:self];
        manager.delegate = self;
        [manager loadPassword];
        _manager = manager;
        
    }
    
}

#pragma mark - WeXPasswordManagerDelegate
- (void)passwordManagerVerifySuccess{
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self createSuccessFloatView];

    });
    
}

//点击备份
- (void)passwordModifySuccessFloatViewDidBackup{
    WeXPassportBackupViewController *ctrl = [[WeXPassportBackupViewController alloc] init];
    [self.navigationController pushViewController:ctrl animated:YES];
}





@end
