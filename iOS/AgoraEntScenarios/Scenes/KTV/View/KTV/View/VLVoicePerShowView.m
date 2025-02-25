//
//  VLVoicePerShowView.m
//  AgoraEntScenarios
//
//  Created by CP on 2023/3/3.
//

#import "VLVoicePerShowView.h"
#import "AESMacro.h"
#import "VLMacroDefine.h"

@interface VLVoicePerShowView()
@property(nonatomic, weak) id <VLVoicePerShowViewDelegate>delegate;
@property (nonatomic,strong) UISwitch *voiceSwitch;
@property (nonatomic,strong) UISwitch *delaySwitch;
@property (nonatomic,strong) UISwitch *aecSwitch;
@property (nonatomic, assign) NSInteger aecGrade;
@property (nonatomic, assign) NSInteger volGrade;
@property (nonatomic, strong) UISegmentedControl *qualitySegment;
@property (nonatomic, strong) UISegmentedControl *volSegment;
@property (nonatomic, strong) UILabel *qualityLabel;
@property (nonatomic, strong) UITextField *aecTF;
@property (nonatomic, assign) BOOL isRoomOwner;
@property (nonatomic, assign) BOOL isProfessional;
@property (nonatomic, assign) BOOL isDelay;
@property (nonatomic, assign) BOOL aecState; //AIAEC switch
@property (nonatomic, assign) NSInteger aecLevel; //AEC level
@end

@implementation VLVoicePerShowView

- (instancetype)initWithFrame:(CGRect)frame isProfessional:(BOOL)isProfessional aecState:(BOOL)state aecLevel:(NSInteger)level isDelay:(BOOL)isDelay isRoomOwner:(BOOL)isRoomOwner volGrade:(NSInteger)vol aecGrade:(NSInteger)grade withDelegate:(id<VLVoicePerShowViewDelegate>)delegate {
    if (self = [super initWithFrame:frame]) {
        self.backgroundColor = UIColorMakeWithHex(@"#152164");
        self.delegate = delegate;
        self.aecGrade = grade;
        self.volGrade = vol;
        self.isRoomOwner = isRoomOwner;
        self.isProfessional = isProfessional;
        self.isDelay = isDelay;
        self.aecLevel = level;
        self.aecState = state;
        [self layoutUI];
    }
    return self;
}

-(void)layoutUI {
    UILabel *titleLabel = [[UILabel alloc]initWithFrame:CGRectMake((SCREEN_WIDTH-200)*0.5, 20, 200, 22)];
    titleLabel.text = KTVLocalizedString(@"ktv_per_setting");
    titleLabel.font = UIFontMake(18);
    titleLabel.textAlignment = NSTextAlignmentCenter;
    titleLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:titleLabel];
    
    UILabel *headLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 60, 80, 30)];
    headLabel.text = KTVLocalizedString(@"ktv_per_style");
    headLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:headLabel];
    
    self.voiceSwitch = [[UISwitch alloc]initWithFrame:CGRectMake(SCREEN_WIDTH - 70, 60, 50, 30)];
    self.voiceSwitch.onTintColor = UIColorMakeWithHex(@"#099DFD");
    [self.voiceSwitch addTarget:self action:@selector(perChange:) forControlEvents:UIControlEventValueChanged];
    self.voiceSwitch.on = self.isProfessional;
    [self addSubview:_voiceSwitch];
    
    UIView *sepView3 = [[UIView alloc]initWithFrame:CGRectMake(20, 105, SCREEN_WIDTH - 40 , 1)];
    sepView3.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    [self addSubview:sepView3];
    
    UILabel *volLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 123, 150, 30)];
    volLabel.text = KTVLocalizedString(@"ktv_per_vol_quality");
    volLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:volLabel];
    
    [self initVolSegmentedControl];
    
    UIView *sepView = [[UIView alloc]initWithFrame:CGRectMake(20, 168, SCREEN_WIDTH - 40 , 1)];
    sepView.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    [self addSubview:sepView];
    
    UILabel *qualityLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 186, 150, 30)];
    qualityLabel.text = KTVLocalizedString(@"ktv_per_ans");
    qualityLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:qualityLabel];
    _qualityLabel = qualityLabel;
    
    [self initSegmentedControl];
    
    UIView *sepView5 = [[UIView alloc]initWithFrame:CGRectMake(20, 231, SCREEN_WIDTH - 40 , 1)];
    sepView5.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    [self addSubview:sepView5];
    
    UILabel *AECLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 249, 150, 30)];
    AECLabel.text = KTVLocalizedString(@"ktv_aiaec_switch");
    AECLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:AECLabel];
    
    self.aecSwitch = [[UISwitch alloc]initWithFrame:CGRectMake(SCREEN_WIDTH - 70, 249, 50, 30)];
    self.aecSwitch.onTintColor = UIColorMakeWithHex(@"#099DFD");
    [self.aecSwitch addTarget:self action:@selector(aecChange:) forControlEvents:UIControlEventValueChanged];
    self.aecSwitch.on = self.aecState;
    [self addSubview:_aecSwitch];
    
    UIView *sepView6 = [[UIView alloc]initWithFrame:CGRectMake(20, 294, SCREEN_WIDTH - 40 , 1)];
    sepView6.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    [self addSubview:sepView6];
    
    UILabel *AECGradeLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 312, 150, 30)];
    AECGradeLabel.text = KTVLocalizedString(@"ktv_aiaec_level");
    AECGradeLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:AECGradeLabel];
    
    _aecTF = [[UITextField alloc]initWithFrame:CGRectMake(200, 312, 80, 30)];
    _aecTF.text = [NSString stringWithFormat:@"%li", (long)_aecLevel];
    _aecTF.textColor = [UIColor whiteColor];
    [self addSubview:_aecTF];
    
    UIButton *aecSetBtn = [[UIButton alloc]initWithFrame:CGRectMake(300, 312, 80, 30)];
    [aecSetBtn setTitle: KTVLocalizedString(@"ktv_perSetting") forState:UIControlStateNormal];
    [aecSetBtn addTarget:self action:@selector(aecSet) forControlEvents:UIControlEventTouchUpInside];
    [self addSubview:aecSetBtn];
    
    UIView *sepView2 = [[UIView alloc]initWithFrame:CGRectMake(20, 357, SCREEN_WIDTH - 40 , 1)];
    sepView2.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    [self addSubview:sepView2];
    
    UILabel *delayLabel = [[UILabel alloc]initWithFrame:CGRectMake(20, 375, 100, 30)];
    delayLabel.text = KTVLocalizedString(@"ktv_low_delay_mode");
    delayLabel.textColor = UIColorMakeWithHex(@"#EFF4FF");
    [self addSubview:delayLabel];
    
    self.delaySwitch = [[UISwitch alloc]initWithFrame:CGRectMake(SCREEN_WIDTH - 70, 375, 50, 30)];
    self.delaySwitch.onTintColor = UIColorMakeWithHex(@"#099DFD");
    self.delaySwitch.on = self.isDelay;
    [self.delaySwitch addTarget:self action:@selector(delayChange:) forControlEvents:UIControlEventValueChanged];
    [self addSubview:_delaySwitch];
    
    if(!self.isRoomOwner) {
        self.delaySwitch.hidden = true;
        delayLabel.hidden = true;
    }

}

-(void)aecChange:(UISwitch *)swich {
    if([self.delegate respondsToSelector:@selector(didAECStateChange:)]){
        [self.delegate didAECStateChange:swich.isOn];
    }
}

-(void)aecSet{
    [_aecTF resignFirstResponder];
    if([self.delegate respondsToSelector:@selector(didAECLevelSetWith:)]){
        [self.delegate didAECLevelSetWith:[_aecTF.text intValue]];
    }
}

-(void)perChange:(UISwitch *)voiceSwitch {
    if([self.delegate respondsToSelector:@selector(voicePerItemSelectedAction:)]){
        [self.delegate voicePerItemSelectedAction:voiceSwitch.isOn];
    }
}

-(void)delayChange:(UISwitch *)delaySwitch {
    self.isDelay = delaySwitch.on;
    if(self.isDelay){
        self.qualitySegment.selectedSegmentIndex = 0;
        self.aecGrade = 0;
        if([self.delegate respondsToSelector:@selector(didAIAECGradeChangedWithIndex:)]){
            [self.delegate didAIAECGradeChangedWithIndex:self.aecGrade];
        }
    }
    if([self.delegate respondsToSelector:@selector(voiceDelaySelectedAction:)]){
        [self.delegate voiceDelaySelectedAction:delaySwitch.isOn];
    }
}

//Initialize Segmented controls
- (void)initSegmentedControl
{
    NSArray *segmentedData = [[NSArray alloc]initWithObjects:KTVLocalizedString(@"ktv_close_aec"),KTVLocalizedString(@"ktv_aec_mid"),KTVLocalizedString(@"ktv_aec_high"),nil];
    self.qualitySegment = [[UISegmentedControl alloc]initWithItems:segmentedData];
    self.qualitySegment.frame = CGRectMake(SCREEN_WIDTH - 209, 184, 189, 34);
    //This is the color when setting the button is pressed.
    self.qualitySegment.selectedSegmentTintColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0.2];
    //Default selected button index
    self.qualitySegment.selectedSegmentIndex = self.aecGrade;
    self.qualitySegment.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    NSDictionary *attributes = [NSDictionary dictionaryWithObjectsAndKeys:[UIFont systemFontOfSize:14],NSFontAttributeName,[UIColor whiteColor], NSForegroundColorAttributeName, nil];
    [self.qualitySegment setTitleTextAttributes:attributes forState:UIControlStateNormal];
    NSDictionary *selectedAttributes = [NSDictionary dictionaryWithObject:[UIColor whiteColor] forKey:NSForegroundColorAttributeName];
    [self.qualitySegment setTitleTextAttributes:selectedAttributes forState:UIControlStateSelected];
    //Set the segmented control and click the corresponding event
    [_qualitySegment addTarget:self action:@selector(segmentSelect:)forControlEvents:UIControlEventValueChanged];
    //Add to the view
    [self addSubview:self.qualitySegment];
}

// Initialize the sound quality Segmented control
- (void)initVolSegmentedControl
{
    NSArray *segmentedData = [[NSArray alloc]initWithObjects:KTVLocalizedString(@"ktv_normal_vol"),KTVLocalizedString(@"ktv_high_vol"),nil];
    self.volSegment = [[UISegmentedControl alloc]initWithItems:segmentedData];
    self.volSegment.frame = CGRectMake(SCREEN_WIDTH - 309, 121, 289, 34);
    //This is the color when setting the button is pressed.
    self.volSegment.selectedSegmentTintColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0.2];
    //Default selected button index
    self.volSegment.selectedSegmentIndex = self.volGrade;
    self.volSegment.backgroundColor = [UIColor colorWithRed:0.938 green:0.938 blue:0.938 alpha:0.08];
    NSDictionary *attributes = [NSDictionary dictionaryWithObjectsAndKeys:[UIFont systemFontOfSize:14],NSFontAttributeName,[UIColor whiteColor], NSForegroundColorAttributeName, nil];
    [self.volSegment setTitleTextAttributes:attributes forState:UIControlStateNormal];
    NSDictionary *selectedAttributes = [NSDictionary dictionaryWithObject:[UIColor whiteColor] forKey:NSForegroundColorAttributeName];
    [self.volSegment setTitleTextAttributes:selectedAttributes forState:UIControlStateSelected];
    //Set the segmented control and click the corresponding event
    [_volSegment addTarget:self action:@selector(volSegmentSelect:)forControlEvents:UIControlEventValueChanged];
    //Add to the view
    [self addSubview:self.volSegment];

}

-(void)volSegmentSelect:(UISegmentedControl *)seg{
    self.volGrade = seg.selectedSegmentIndex;
    if([self.delegate respondsToSelector:@selector(didVolQualityGradeChangedWithIndex:)]){
        [self.delegate didVolQualityGradeChangedWithIndex:self.volGrade];
    }
}

-(void)segmentSelect:(UISegmentedControl *)seg{
    self.aecGrade = seg.selectedSegmentIndex;
    if(self.aecGrade > 0){
        self.isDelay = false;
        self.delaySwitch.on = false;
        if([self.delegate respondsToSelector:@selector(voiceDelaySelectedAction:)]){
            [self.delegate voiceDelaySelectedAction:false];
        }
    }
    if([self.delegate respondsToSelector:@selector(didAIAECGradeChangedWithIndex:)]){
        [self.delegate didAIAECGradeChangedWithIndex:self.aecGrade];
    }
}

@end
