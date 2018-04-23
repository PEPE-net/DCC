//
//  WeXWalletAllTranstionDetailView.h
//  WeXBlockChain
//
//  Created by wcc on 2018/3/19.
//  Copyright © 2018年 WeX. All rights reserved.
//

#import <UIKit/UIKit.h>

typedef void(^CancelBtnBlock)(void);
typedef void(^ConfirmBtnBlock)(void);

@interface WeXWalletAllTranstionDetailView : UIView

@property (nonatomic,strong)UILabel *toLabel;
@property (nonatomic,strong)UILabel *fromLabel;
@property (nonatomic,strong)UILabel *valueLabel;
@property (nonatomic,strong)UILabel *costLabel;
@property (nonatomic,strong)UILabel *balanceLabel;
@property (nonatomic,strong)UILabel *balanceEthLabel;
@property (nonatomic,strong)UILabel *receiveLabel;
@property (nonatomic,strong)UILabel *feeLabel;

@property (nonatomic,copy)CancelBtnBlock cancelBtnBlock;
@property (nonatomic,copy)ConfirmBtnBlock confirmBtnBlock;

- (void)dismiss;

@end

